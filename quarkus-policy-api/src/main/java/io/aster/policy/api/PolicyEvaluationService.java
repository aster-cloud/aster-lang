package io.aster.policy.api;

import io.quarkus.cache.CacheResult;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Policy evaluation service with caching support (Reactive)
 *
 * 该服务负责策略评估并使用Caffeine缓存来提升性能。
 * 使用Mutiny的Uni实现reactive编程模型。
 * 缓存key基于策略模块、函数名和输入参数的哈希值。
 */
@ApplicationScoped
public class PolicyEvaluationService {

    // Cache for compiled policy metadata (avoids repeated reflection)
    private final ConcurrentHashMap<String, PolicyMetadata> metadataCache = new ConcurrentHashMap<>();

    // Cache for constructor metadata (avoids repeated reflection for object construction)
    private final ConcurrentHashMap<Class<?>, ConstructorMetadata> constructorCache = new ConcurrentHashMap<>();

    /**
     * Cached metadata for a policy method
     */
    private static class PolicyMetadata {
        final Class<?> policyClass;
        final Method method;
        final MethodHandle methodHandle;
        final Parameter[] parameters;

        PolicyMetadata(Class<?> policyClass, Method method, MethodHandle methodHandle, Parameter[] parameters) {
            this.policyClass = policyClass;
            this.method = method;
            this.methodHandle = methodHandle;
            this.parameters = parameters;
        }
    }

    /**
     * Cached constructor metadata for fast object construction
     */
    private static class ConstructorMetadata {
        final java.lang.reflect.Constructor<?> constructor;
        final Parameter[] parameters;
        final java.lang.reflect.Field[] fields;

        ConstructorMetadata(java.lang.reflect.Constructor<?> constructor, Parameter[] parameters, java.lang.reflect.Field[] fields) {
            this.constructor = constructor;
            this.parameters = parameters;
            this.fields = fields;
        }
    }

    /**
     * 评估策略（带缓存，reactive版本，优化了反射性能）
     *
     * @param policyModule 策略模块名
     * @param policyFunction 策略函数名
     * @param context 上下文参数
     * @return Uni包装的评估结果
     */
    public Uni<PolicyEvaluationResult> evaluatePolicy(
            String policyModule,
            String policyFunction,
            Object[] context) {

        // Create cache key
        PolicyCacheKey cacheKey = new PolicyCacheKey(policyModule, policyFunction, context);
        return evaluatePolicyWithKey(cacheKey);
    }

    /**
     * Internal method with cache key for proper caching
     */
    @CacheResult(cacheName = "policy-results")
    Uni<PolicyEvaluationResult> evaluatePolicyWithKey(PolicyCacheKey cacheKey) {

        return Uni.createFrom().item(() -> {
            try {
                long startTime = System.nanoTime();

                // Get or load policy metadata (cached)
                String metadataKey = cacheKey.getPolicyModule() + "." + cacheKey.getPolicyFunction();
                PolicyMetadata metadata = metadataCache.computeIfAbsent(metadataKey,
                    key -> loadPolicyMetadata(cacheKey.getPolicyModule(), cacheKey.getPolicyFunction()));

                // 准备参数
                Object[] args = prepareArguments(metadata.parameters, cacheKey.getContext());

                // 使用MethodHandle调用策略 (比reflection快2-3倍)
                Object result = metadata.methodHandle.invokeWithArguments(args);
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
    public Uni<Void> invalidateCache(String policyModule, String policyFunction, Object[] context) {
        PolicyCacheKey cacheKey = new PolicyCacheKey(policyModule, policyFunction, context);
        return invalidateCacheWithKey(cacheKey);
    }

    /**
     * Internal method with cache key for cache invalidation
     */
    @CacheInvalidate(cacheName = "policy-results")
    Uni<Void> invalidateCacheWithKey(PolicyCacheKey cacheKey) {
        // 返回completed Uni，缓存失效由注解处理
        return Uni.createFrom().voidItem();
    }

    /**
     * 批量评估多个策略（并行执行，显著提升吞吐量）
     *
     * @param requests 策略评估请求列表
     * @return Uni包装的评估结果列表
     */
    public Uni<java.util.List<PolicyEvaluationResult>> evaluateBatch(
            java.util.List<BatchRequest> requests) {

        // 并行执行所有策略评估
        java.util.List<Uni<PolicyEvaluationResult>> unis = requests.stream()
            .map(req -> evaluatePolicy(req.policyModule, req.policyFunction, req.context))
            .collect(java.util.stream.Collectors.toList());

        // 合并所有Uni结果使用现代API
        return Uni.join().all(unis).andFailFast();
    }

    /**
     * 批量请求数据类
     */
    public static class BatchRequest {
        public String policyModule;
        public String policyFunction;
        public Object[] context;

        public BatchRequest() {}

        public BatchRequest(String policyModule, String policyFunction, Object[] context) {
            this.policyModule = policyModule;
            this.policyFunction = policyFunction;
            this.context = context;
        }
    }

    /**
     * 清空所有缓存（reactive版本）
     */
    @CacheInvalidateAll(cacheName = "policy-results")
    public Uni<Void> clearAllCache() {
        // 同时清空元数据缓存，允许重新加载策略类
        metadataCache.clear();
        constructorCache.clear();
        // 返回completed Uni，缓存清空由注解处理
        return Uni.createFrom().voidItem();
    }

    /**
     * 准备方法参数
     */
    private Object[] prepareArguments(Parameter[] parameters, Object[] context) throws Exception {
        if (context == null || parameters.length == 0) {
            return new Object[0];
        }

        Object[] args = new Object[parameters.length];
        for (int i = 0; i < Math.min(parameters.length, context.length); i++) {
            Class<?> expectedType = parameters[i].getType();
            Object contextObj = context[i];

            if (contextObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) contextObj;

                // 对于基本类型和包装类型，直接从map中获取值
                if (isPrimitiveOrWrapper(expectedType)) {
                    // 假设map只有一个键值对，或者使用约定的键
                    Object value = map.values().iterator().next();
                    args[i] = convertValue(value, expectedType);
                } else {
                    args[i] = constructFromMap(expectedType, map);
                }
            } else {
                // 直接传递的值，可能需要类型转换
                args[i] = convertValue(contextObj, expectedType);
            }
        }
        return args;
    }

    /**
     * 判断是否为基本类型或其包装类
     */
    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() ||
               type == Integer.class || type == Long.class ||
               type == Double.class || type == Float.class ||
               type == Boolean.class || type == Character.class ||
               type == Byte.class || type == Short.class ||
               type == String.class;
    }

    /**
     * 从Map构造对象（使用缓存的构造器元数据）
     */
    private Object constructFromMap(Class<?> targetClass, Map<String, Object> map) throws Exception {
        // Get or cache constructor metadata
        ConstructorMetadata metadata = constructorCache.computeIfAbsent(targetClass, key -> {
            var constructors = key.getConstructors();
            if (constructors.length == 0) {
                throw new IllegalArgumentException(
                    "No public constructors found for " + key.getName());
            }
            var constructor = constructors[0];
            return new ConstructorMetadata(
                constructor,
                constructor.getParameters(),
                key.getDeclaredFields()
            );
        });

        Object[] args = new Object[metadata.parameters.length];

        for (int i = 0; i < metadata.parameters.length && i < metadata.fields.length; i++) {
            String fieldName = metadata.fields[i].getName();
            Object value = map.get(fieldName);
            Class<?> paramType = metadata.parameters[i].getType();

            if (value != null) {
                args[i] = convertValue(value, paramType);
            } else {
                args[i] = getDefaultValue(paramType);
            }
        }

        return metadata.constructor.newInstance(args);
    }

    /**
     * 类型转换（提取为独立方法以便JIT优化）
     */
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return getDefaultValue(targetType);
        }

        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(value)) {
            return value;
        }

        // 数字类型转换
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == int.class || targetType == Integer.class) {
                return num.intValue();
            } else if (targetType == long.class || targetType == Long.class) {
                return num.longValue();
            } else if (targetType == double.class || targetType == Double.class) {
                return num.doubleValue();
            } else if (targetType == float.class || targetType == Float.class) {
                return num.floatValue();
            } else if (targetType == short.class || targetType == Short.class) {
                return num.shortValue();
            } else if (targetType == byte.class || targetType == Byte.class) {
                return num.byteValue();
            }
        }

        // 布尔类型转换
        if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Boolean) {
                return value;
            }
            return Boolean.parseBoolean(value.toString());
        }

        // 字符串转换
        if (targetType == String.class) {
            return value.toString();
        }

        // 其他情况直接返回
        return value;
    }

    /**
     * 获取基本类型的默认值
     */
    private Object getDefaultValue(Class<?> type) {
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == boolean.class) return false;
        return null;
    }

    /**
     * Policy evaluation result
     */
    public static class PolicyEvaluationResult {
        private final Object result;
        private final double executionTimeMs;
        private final boolean fromCache;

        public PolicyEvaluationResult(Object result, double executionTimeMs, boolean fromCache) {
            this.result = result;
            this.executionTimeMs = executionTimeMs;
            this.fromCache = fromCache;
        }

        public Object getResult() {
            return result;
        }

        public double getExecutionTimeMs() {
            return executionTimeMs;
        }

        public boolean isFromCache() {
            return fromCache;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PolicyEvaluationResult that = (PolicyEvaluationResult) o;
            return Double.compare(that.executionTimeMs, executionTimeMs) == 0 &&
                   fromCache == that.fromCache &&
                   Objects.equals(result, that.result);
        }

        @Override
        public int hashCode() {
            return Objects.hash(result, executionTimeMs, fromCache);
        }
    }
}
