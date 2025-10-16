package io.aster.policy.api.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.aster.policy.api.PolicyCacheKey;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 策略缓存管理器，封装生命周期跟踪与租户索引。
 */
@ApplicationScoped
public class PolicyCacheManager {

    private static final Logger LOG = Logger.getLogger(PolicyCacheManager.class);

    @Inject
    @CacheName("policy-results")
    Cache policyResultCache;

    private CaffeineCache caffeineCacheDelegate;

    private com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, Boolean> cacheLifecycleTracker;

    private final ConcurrentHashMap<String, Set<PolicyCacheKey>> tenantCacheIndex = new ConcurrentHashMap<>();

    @PostConstruct
    void initCacheLifecycleTracking() {
        Duration expireAfterWrite = null;
        Long maximumSize = null;
        Integer initialCapacity = null;

        try {
            caffeineCacheDelegate = policyResultCache.as(CaffeineCache.class);
            if (caffeineCacheDelegate instanceof io.quarkus.cache.runtime.caffeine.CaffeineCacheImpl impl) {
                var info = impl.getCacheInfo();
                initialCapacity = info.initialCapacity;
                maximumSize = info.maximumSize;
                expireAfterWrite = info.expireAfterWrite;
            }
        } catch (IllegalStateException ex) {
            LOG.warn("政策评估缓存未使用Caffeine后端，租户索引将使用默认监听配置", ex);
            caffeineCacheDelegate = null;
        }

        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if (initialCapacity != null) {
            builder.initialCapacity(initialCapacity);
        }
        if (maximumSize != null) {
            builder.maximumSize(maximumSize);
        }
        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite);
        }

        cacheLifecycleTracker = builder.removalListener((PolicyCacheKey removedKey, Boolean ignored, RemovalCause cause) -> {
            if (removedKey != null) {
                removeTrackedKey(removedKey);
                if (LOG.isDebugEnabled()) {
                    LOG.debugf("Caffeine移除缓存键, cause=%s, key=%s", cause, removedKey);
                }
            }
        }).build();
    }

    public Cache getPolicyResultCache() {
        return policyResultCache;
    }

    public CaffeineCache getCaffeineCacheDelegate() {
        return caffeineCacheDelegate;
    }

    public com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, Boolean> getLifecycleTracker() {
        return cacheLifecycleTracker;
    }

    public com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, ?> getNativePolicyCache() {
        if (caffeineCacheDelegate == null) {
            return null;
        }
        try {
            var getCache = caffeineCacheDelegate.getClass().getDeclaredMethod("getCache");
            getCache.setAccessible(true);
            Object cache = getCache.invoke(caffeineCacheDelegate);
            if (cache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                @SuppressWarnings("unchecked")
                com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, ?> typed =
                    (com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, ?>) caffeineCache;
                return typed;
            }
        } catch (NoSuchMethodException ignored) {
            // ignore and try field scan
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("通过反射获取底层Caffeine缓存失败", e);
            }
        }
        try {
            for (var field : caffeineCacheDelegate.getClass().getDeclaredFields()) {
                if (com.github.benmanes.caffeine.cache.Cache.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(caffeineCacheDelegate);
                    if (value instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineCache) {
                        @SuppressWarnings("unchecked")
                        com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, ?> typed =
                            (com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, ?>) caffeineCache;
                        return typed;
                    }
                }
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("通过字段访问底层Caffeine缓存失败", e);
            }
        }
        return null;
    }

    public void registerCacheEntry(PolicyCacheKey key, String tenantId) {
        if (key == null) {
            return;
        }
        String normalizedTenant = normalizeTenant(tenantId != null ? tenantId : key.getTenantId());
        tenantCacheIndex
            .computeIfAbsent(normalizedTenant, k -> ConcurrentHashMap.newKeySet())
            .add(key);
        if (cacheLifecycleTracker != null) {
            cacheLifecycleTracker.put(key, Boolean.TRUE);
        }
    }

    public void removeCacheEntry(PolicyCacheKey key, String tenantId) {
        if (key == null) {
            return;
        }
        if (cacheLifecycleTracker != null) {
            cacheLifecycleTracker.invalidate(key);
        }
        removeTrackedKey(key, tenantId);
    }

    public boolean isCacheHit(PolicyCacheKey key) {
        if (key == null) {
            return false;
        }
        String normalizedTenant = normalizeTenant(key.getTenantId());
        Set<PolicyCacheKey> keys = tenantCacheIndex.get(normalizedTenant);
        return keys != null && keys.contains(key);
    }

    public Set<PolicyCacheKey> snapshotTenantCacheKeys(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        Set<PolicyCacheKey> keys = tenantCacheIndex.get(normalizedTenant);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<>(keys));
    }

    public void invalidateTenantCache(String tenantId) {
        String normalizedTenant = normalizeTenant(tenantId);
        Set<PolicyCacheKey> keys = tenantCacheIndex.remove(normalizedTenant);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        if (cacheLifecycleTracker != null) {
            for (PolicyCacheKey key : keys) {
                cacheLifecycleTracker.invalidate(key);
            }
        }
    }

    public void clearAllCache() {
        tenantCacheIndex.clear();
        if (cacheLifecycleTracker != null) {
            cacheLifecycleTracker.invalidateAll();
        }
    }

    private void removeTrackedKey(PolicyCacheKey cacheKey) {
        removeTrackedKey(cacheKey, cacheKey != null ? cacheKey.getTenantId() : null);
    }

    private void removeTrackedKey(PolicyCacheKey cacheKey, String tenantId) {
        if (cacheKey == null) {
            return;
        }
        String normalizedTenant = normalizeTenant(tenantId != null ? tenantId : cacheKey.getTenantId());
        Set<PolicyCacheKey> keys = tenantCacheIndex.get(normalizedTenant);
        if (keys != null) {
            keys.remove(cacheKey);
            if (keys.isEmpty()) {
                tenantCacheIndex.remove(normalizedTenant, keys);
            }
        }
    }

    private String normalizeTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? "default" : tenantId.trim();
    }
}
