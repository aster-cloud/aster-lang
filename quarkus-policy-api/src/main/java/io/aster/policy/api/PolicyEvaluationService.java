package io.aster.policy.api;

import io.aster.policy.api.cache.PolicyCacheManager;
import io.aster.policy.api.convert.PolicyTypeConverter;
import io.aster.policy.api.model.BatchEvaluationResult;
import io.aster.policy.api.model.BatchRequest;
import io.aster.policy.api.model.CompositionStep;
import io.aster.policy.api.model.EvaluationAttempt;
import io.aster.policy.api.model.ParameterInfo;
import io.aster.policy.api.model.PolicyCompositionResult;
import io.aster.policy.api.model.PolicyEvaluationResult;
import io.aster.policy.api.model.PolicyMetadata;
import io.aster.policy.api.model.PolicyValidationResult;
import io.aster.policy.api.model.StepResult;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheInvalidateAll;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

/**
 * Policy evaluation service with caching support (Reactive)
 *
 * 该服务负责策略评估并使用Caffeine缓存来提升性能。
 * 使用Mutiny的Uni实现reactive编程模型。
 * 缓存key基于策略模块、函数名和输入参数的哈希值。
 */
@ApplicationScoped
public class PolicyEvaluationService {

    @Inject
    PolicyCacheManager policyCacheManager;

    @Inject
    PolicyTypeConverter policyTypeConverter;

    private static final Logger LOG = Logger.getLogger(PolicyEvaluationService.class);

    // Cache for compiled policy metadata (avoids repeated reflection)
    private final ConcurrentHashMap<String, PolicyMetadata> metadataCache = new ConcurrentHashMap<>();
    // constructor metadata cache moved to PolicyTypeConverter

    /**
     * 评估策略（带缓存，reactive版本，优化了反射性能）
     *
     * @param policyModule 策略模块名
     * @param policyFunction 策略函数名
     * @param context 上下文参数
     * @return Uni包装的评估结果
     */
    public Uni<PolicyEvaluationResult> evaluatePolicy(
            String tenantId,
            String policyModule,
            String policyFunction,
            Object[] context) {

        Object[] normalizedContext = normalizeContext(tenantId, context);
        final PolicyCacheKey cacheKey = new PolicyCacheKey(tenantId, policyModule, policyFunction, normalizedContext);

        // 使用底层Caffeine缓存探测命中状态，避免fromCache标记失真
        Cache cache = policyCacheManager.getPolicyResultCache();
        final boolean cacheHit = policyCacheManager.isCacheHit(cacheKey);

        return cache
            .getAsync(cacheKey, this::evaluatePolicyWithKey)
            .invoke(result -> policyCacheManager.registerCacheEntry(cacheKey, tenantId))
            .onItem().transform(result -> adjustFromCacheFlag(result, cacheHit))
            .onFailure().invoke(throwable -> {
                if (!cacheHit) {
                    policyCacheManager.removeCacheEntry(cacheKey, tenantId);
                }
            });
    }

    /**
     * Internal method with cache key for proper caching
     */
    Uni<PolicyEvaluationResult> evaluatePolicyWithKey(PolicyCacheKey cacheKey) {

        return Uni.createFrom().item(() -> {
            try {
                long startTime = System.nanoTime();

                // Get or load policy metadata (cached)
                String metadataKey = cacheKey.getPolicyModule() + "." + cacheKey.getPolicyFunction();
                PolicyMetadata metadata = metadataCache.computeIfAbsent(metadataKey,
                    key -> loadPolicyMetadata(cacheKey.getPolicyModule(), cacheKey.getPolicyFunction()));

                // 准备参数
                Object[] args = policyTypeConverter.prepareArguments(metadata.getParameters(), cacheKey.getContext());

                // 使用MethodHandle调用策略 (比reflection快2-3倍)
                Object result = metadata.getMethodHandle().invokeWithArguments(args);
                long durationNanos = System.nanoTime() - startTime;

                // 构建结果
                return new PolicyEvaluationResult(
                    result,
                    durationNanos / 1_000_000.0,
                    false // 第一次执行，不是从缓存获取
                );
            } catch (Throwable e) {
                throw new RuntimeException("Policy evaluation failed", e);
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    /**
     * 加载策略元数据（仅首次调用时执行）
     */
    private PolicyMetadata loadPolicyMetadata(String policyModule, String policyFunction) {
        try {
            // 动态加载策略类
            String className = policyModule + "." + policyFunction + "_fn";
            Class<?> policyClass = Class.forName(className);

            // 查找函数方法
            Method functionMethod = null;
            for (Method m : policyClass.getDeclaredMethods()) {
                if (m.getName().equals(policyFunction) &&
                    java.lang.reflect.Modifier.isStatic(m.getModifiers())) {
                    functionMethod = m;
                    break;
                }
            }

            if (functionMethod == null) {
                throw new IllegalArgumentException(
                    "Policy method not found: " + policyFunction);
            }

            // 创建MethodHandle用于快速调用
            MethodHandle methodHandle = MethodHandles.lookup().unreflect(functionMethod);

            return new PolicyMetadata(
                policyClass,
                functionMethod,
                methodHandle,
                functionMethod.getParameters()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load policy metadata: " + policyModule + "." + policyFunction, e);
        }
    }

    /**
     * 使缓存失效（针对特定策略，reactive版本）
     */
    public Uni<Void> invalidateCache(String tenantId, String policyModule, String policyFunction, Object[] context) {
        Object[] normalizedContext = normalizeContext(tenantId, context);
        PolicyCacheKey cacheKey = new PolicyCacheKey(tenantId, policyModule, policyFunction, normalizedContext);
        return invalidateCacheWithKey(cacheKey);
    }

    /**
     * 按租户维度批量失效缓存，可选过滤模块与函数
     */
    public Uni<Void> invalidateCache(String tenantId, String policyModule, String policyFunction) {
        Set<PolicyCacheKey> keys = policyCacheManager.snapshotTenantCacheKeys(tenantId);
        if (keys.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        boolean filterByModule = policyModule != null && !policyModule.isBlank();
        boolean filterByFunction = policyFunction != null && !policyFunction.isBlank();

        java.util.List<PolicyCacheKey> targets = keys.stream()
            .filter(key -> !filterByModule || Objects.equals(key.getPolicyModule(), policyModule))
            .filter(key -> !filterByFunction || Objects.equals(key.getPolicyFunction(), policyFunction))
            .collect(java.util.stream.Collectors.toList());

        if (targets.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        java.util.List<Uni<Void>> invalidations = new java.util.ArrayList<>();
        Cache cache = policyCacheManager.getPolicyResultCache();
        for (PolicyCacheKey key : targets) {
            invalidations.add(cache.invalidate(key)
                .invoke(() -> policyCacheManager.removeCacheEntry(key, key.getTenantId())));
        }

        return Uni.combine().all().unis(invalidations).discardItems();
    }

    /**
     * 仅用于测试：返回指定租户当前缓存键快照
     */
    public Set<PolicyCacheKey> snapshotTenantCacheKeys(String tenantId) {
        return policyCacheManager.snapshotTenantCacheKeys(tenantId);
    }

    /**
     * Internal method with cache key for cache invalidation
     */
    Uni<Void> invalidateCacheWithKey(PolicyCacheKey cacheKey) {
        return policyCacheManager.getPolicyResultCache().invalidate(cacheKey)
            .invoke(() -> policyCacheManager.removeCacheEntry(cacheKey, cacheKey.getTenantId()));
    }

    private Object[] normalizeContext(String tenantId, Object[] context) {
        if (context == null || context.length == 0) {
            return new Object[0];
        }

        Object[] copy = Arrays.copyOf(context, context.length);
        Object first = copy[0];
        String normalizedTenant = normalizeTenant(tenantId);
        if (first instanceof String tenantMarker &&
                normalizeTenant(tenantMarker).equals(normalizedTenant)) {
            return Arrays.copyOfRange(copy, 1, copy.length);
        }

        return copy;
    }

    private String normalizeTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }

    private PolicyEvaluationResult adjustFromCacheFlag(PolicyEvaluationResult original, boolean fromCache) {
        if (original == null || original.isFromCache() == fromCache) {
            return original;
        }
        return new PolicyEvaluationResult(
            original.getResult(),
            original.getExecutionTimeMs(),
            fromCache
        );
    }

    /**
     * 批量评估多个策略（并行执行，显著提升吞吐量，任一失败则全部失败）
     *
     * @param requests 策略评估请求列表
     * @return Uni包装的评估结果列表
     */
    public Uni<java.util.List<PolicyEvaluationResult>> evaluateBatch(
            java.util.List<BatchRequest> requests) {

        // 并行执行所有策略评估
        java.util.List<Uni<PolicyEvaluationResult>> unis = requests.stream()
            .map(req -> evaluatePolicy(req.tenantId, req.policyModule, req.policyFunction, req.context))
            .collect(java.util.stream.Collectors.toList());

        // 合并所有Uni结果使用现代API（任一失败则全部失败）
        return Uni.join().all(unis).andFailFast();
    }

    /**
     * 批量评估多个策略（并行执行，收集所有成功和失败结果）
     *
     * @param requests 策略评估请求列表
     * @return Uni包装的批量评估结果（包含成功和失败的结果）
     */
    public Uni<BatchEvaluationResult> evaluateBatchWithFailures(
            java.util.List<BatchRequest> requests) {

        // 为每个请求创建带错误处理的Uni
        java.util.List<Uni<EvaluationAttempt>> attempts = new java.util.ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            final int index = i;
            final BatchRequest req = requests.get(i);

            Uni<EvaluationAttempt> attempt = evaluatePolicy(req.tenantId, req.policyModule, req.policyFunction, req.context)
                .onItem().transform(result -> new EvaluationAttempt(
                    index,
                    req.policyModule,
                    req.policyFunction,
                    result,
                    null
                ))
                .onFailure().recoverWithItem(error -> new EvaluationAttempt(
                    index,
                    req.policyModule,
                    req.policyFunction,
                    null,
                    error.getMessage()
                ));

            attempts.add(attempt);
        }

        // 并行执行所有评估（不会因失败而中断）
        return Uni.join().all(attempts).andCollectFailures()
            .onItem().transform(attemptResults -> {
                // 分离成功和失败结果
                java.util.List<EvaluationAttempt> successes = new java.util.ArrayList<>();
                java.util.List<EvaluationAttempt> failures = new java.util.ArrayList<>();

                for (EvaluationAttempt attempt : attemptResults) {
                    if (attempt.getError() == null) {
                        successes.add(attempt);
                    } else {
                        failures.add(attempt);
                    }
                }

                return new BatchEvaluationResult(
                    successes,
                    failures,
                    successes.size(),
                    failures.size(),
                    requests.size()
                );
            });
    }

    /**
     * 策略组合执行（顺序执行多个策略，可选择前一个策略的结果作为下一个策略的输入）
     *
     * @param steps 策略组合步骤列表（按顺序执行）
     * @param initialContext 初始上下文参数
     * @return Uni包装的最终评估结果和中间结果列表
     */
    public Uni<PolicyCompositionResult> evaluateComposition(
            String tenantId,
            java.util.List<CompositionStep> steps,
            Object[] initialContext) {

        if (steps == null || steps.isEmpty()) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Composition steps cannot be empty")
            );
        }

        // 从初始上下文开始，顺序链接所有步骤
        return evaluateCompositionStep(tenantId, steps, 0, initialContext,
            new PolicyCompositionResult(new java.util.ArrayList<>(), null));
    }

    /**
     * 递归执行组合步骤
     */
    private Uni<PolicyCompositionResult> evaluateCompositionStep(
            String tenantId,
            java.util.List<CompositionStep> steps,
            int stepIndex,
            Object[] context,
            PolicyCompositionResult accumulator) {

        if (stepIndex >= steps.size()) {
            return Uni.createFrom().item(accumulator);
        }

        CompositionStep step = steps.get(stepIndex);

        // 执行当前步骤
        return evaluatePolicy(tenantId, step.policyModule, step.policyFunction, context)
            .chain(stepResult -> {
                // 记录步骤结果
                accumulator.getStepResults().add(new StepResult(
                    step.policyModule,
                    step.policyFunction,
                    stepResult.getResult(),
                    stepResult.getExecutionTimeMs(),
                    stepIndex
                ));

                // 更新最终结果
                accumulator.setFinalResult(stepResult.getResult());

                // 确定下一步的上下文
                Object[] nextContext;
                if (step.useResultAsInput && stepIndex < steps.size() - 1) {
                    // 使用当前步骤的结果作为下一步的输入
                    nextContext = new Object[]{stepResult.getResult()};
                } else {
                    // 继续使用原始上下文
                    nextContext = context;
                }

                // 递归执行下一步
                return evaluateCompositionStep(tenantId, steps, stepIndex + 1, nextContext, accumulator);
            });
    }

    /**
     * 清空所有缓存（reactive版本）
     */
    @CacheInvalidateAll(cacheName = "policy-results")
    public Uni<Void> clearAllCache() {
        // 同时清空元数据缓存，允许重新加载策略类
        metadataCache.clear();
        policyCacheManager.clearAllCache();
        // 返回completed Uni，缓存清空由注解处理
        return Uni.createFrom().voidItem();
    }

    /**
     * 验证策略是否存在并可以加载
     *
     * @param policyModule 策略模块名
     * @param policyFunction 策略函数名
     * @return Uni包装的验证结果
     */
    public Uni<PolicyValidationResult> validatePolicy(String policyModule, String policyFunction) {
        return Uni.createFrom().item(() -> {
            try {
                // 尝试加载策略元数据
                String metadataKey = policyModule + "." + policyFunction;
                PolicyMetadata metadata = metadataCache.computeIfAbsent(metadataKey,
                    key -> loadPolicyMetadata(policyModule, policyFunction));

                // 获取参数信息
                java.util.List<ParameterInfo> parameters = new java.util.ArrayList<>();
                for (Parameter param : metadata.getParameters()) {
                    parameters.add(new ParameterInfo(
                        param.getName(),
                        param.getType().getSimpleName(),
                        param.getType().getName()
                    ));
                }

                // 获取返回类型
                String returnType = metadata.getMethod().getReturnType().getSimpleName();
                String returnTypeFullName = metadata.getMethod().getReturnType().getName();

                return new PolicyValidationResult(
                    true,
                    "Policy exists and is valid",
                    policyModule,
                    policyFunction,
                    parameters,
                    returnType,
                    returnTypeFullName
                );
            } catch (Exception e) {
                return new PolicyValidationResult(
                    false,
                    "Policy validation failed: " + e.getMessage(),
                    policyModule,
                    policyFunction,
                    null,
                    null,
                    null
                );
            }
        }).runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

}
