package io.aster.policy.api;

import io.aster.policy.api.cache.PolicyCacheManager;
import io.aster.policy.api.convert.PolicyTypeConverter;
import io.aster.policy.api.validation.QuarkusValidationAdapter;
import io.aster.validation.metadata.ConstructorMetadataCache;
import io.aster.validation.metadata.PolicyMetadataLoader;
import io.smallrye.mutiny.infrastructure.Infrastructure;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PolicyEvaluationService 基准测试专用的轻量化引导器，绕过 Quarkus CDI，在JMH运行时构造所需依赖。
 */
public final class BenchmarkBootstrap implements AutoCloseable {

    private final PolicyEvaluationService service;
    private final PolicyCacheManager cacheManager;
    private final StandalonePolicyCache localCache;
    private final ConstructorMetadataCache constructorMetadataCache;
    private final PolicyMetadataLoader metadataLoader;
    private final Set<String> metadataPrimed;
    private final Set<String> cachePrimed;

    public BenchmarkBootstrap() {
        this.constructorMetadataCache = new ConstructorMetadataCache();
        QuarkusValidationAdapter adapter = new QuarkusValidationAdapter(constructorMetadataCache);
        PolicyTypeConverter typeConverter = new PolicyTypeConverter(constructorMetadataCache, adapter);
        this.metadataLoader = new PolicyMetadataLoader();

        this.cacheManager = new PolicyCacheManager();
        this.localCache = new StandalonePolicyCache("policy-results");
        initializeCacheManager(cacheManager, localCache);

        this.service = new PolicyEvaluationService();
        service.policyCacheManager = cacheManager;
        service.policyTypeConverter = typeConverter;
        service.policyMetadataLoader = metadataLoader;
        service.constructorMetadataCache = constructorMetadataCache;
        service.workflowRuntime = null;
        service.preloadPolicyMetadata();

        this.metadataPrimed = ConcurrentHashMap.newKeySet();
        this.cachePrimed = ConcurrentHashMap.newKeySet();
    }

    private static void initializeCacheManager(PolicyCacheManager manager, StandalonePolicyCache cache) {
        try {
            var cacheField = PolicyCacheManager.class.getDeclaredField("policyResultCache");
            cacheField.setAccessible(true);
            cacheField.set(manager, cache);

            var initMethod = PolicyCacheManager.class.getDeclaredMethod("initCacheLifecycleTracking");
            initMethod.setAccessible(true);
            initMethod.invoke(manager);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("无法初始化PolicyCacheManager缓存", ex);
        }
    }

    public PolicyEvaluationService service() {
        return service;
    }

    public void resetAllCachesAndMetadata() {
        localCache.invalidateAll().await().indefinitely();
        cacheManager.clearAllCache();
        constructorMetadataCache.clear();
        metadataLoader.clear();
        metadataPrimed.clear();
        cachePrimed.clear();
    }

    public boolean markMetadataPrimed(String key) {
        return metadataPrimed.add(key);
    }

    public boolean markCachePrimed(String key) {
        return cachePrimed.add(key);
    }

    @Override
    public void close() {
        try {
            localCache.invalidateAll().await().indefinitely();
        } finally {
            cacheManager.clearAllCache();
            Infrastructure.getDefaultWorkerPool().shutdownNow();
            Infrastructure.reload();
        }
    }
}
