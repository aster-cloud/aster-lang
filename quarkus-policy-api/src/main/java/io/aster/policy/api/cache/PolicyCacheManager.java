package io.aster.policy.api.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import io.aster.policy.api.PolicyCacheKey;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.cache.CaffeineCache;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import io.quarkus.redis.datasource.pubsub.PubSubCommands.RedisSubscriber;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.enterprise.inject.Instance;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * 策略缓存管理器，封装生命周期跟踪与租户索引。
 */
@ApplicationScoped
public class PolicyCacheManager {

    private static final Logger LOG = Logger.getLogger(PolicyCacheManager.class);
    private static final String INVALIDATION_CHANNEL = "policy-cache:invalidate";

    @Inject
    @CacheName("policy-results")
    Cache policyResultCache;

    @Inject
    Instance<RedisDataSource> redisDataSource;

    private CaffeineCache caffeineCacheDelegate;

    private com.github.benmanes.caffeine.cache.Cache<PolicyCacheKey, Boolean> cacheLifecycleTracker;

    private final ConcurrentHashMap<String, Set<PolicyCacheKey>> tenantCacheIndex = new ConcurrentHashMap<>();
    private PubSubCommands<String> pubSubCommands;
    private RedisSubscriber redisSubscriber;
    private ExecutorService invalidationExecutor;

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

        initRedisInvalidationChannel();
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

    private void initRedisInvalidationChannel() {
        if (redisDataSource == null || redisDataSource.isUnsatisfied()) {
            LOG.debug("RedisDataSource 未配置，跳过分布式缓存广播");
            return;
        }
        try {
            invalidationExecutor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "policy-cache-invalidation");
                thread.setDaemon(true);
                return thread;
            });
            pubSubCommands = redisDataSource.get().pubsub(String.class);
            redisSubscriber = pubSubCommands.subscribe(INVALIDATION_CHANNEL, payload ->
                invalidationExecutor.execute(() -> handleRemoteInvalidation(payload))
            );
            LOG.infof("Redis 分布式缓存失效通道已启用: %s", INVALIDATION_CHANNEL);
        } catch (Exception e) {
            LOG.warn("初始化 Redis 缓存失效订阅失败", e);
        }
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
        publishInvalidation(key);
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
        publishInvalidation(normalizedTenant, null, null, null);
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

    private void handleRemoteInvalidation(String payload) {
        if (payload == null || payload.isBlank()) {
            return;
        }
        try {
            JsonObject message = new JsonObject(payload);
            String tenantId = normalizeTenant(message.getString("tenantId"));
            String policyModule = message.getString("policyModule");
            String policyFunction = message.getString("policyFunction");
            Integer hash = message.containsKey("hash") ? message.getInteger("hash") : null;

            if (policyModule == null && policyFunction == null && hash == null) {
                tenantCacheIndex.remove(tenantId);
                return;
            }

            Set<PolicyCacheKey> keys = tenantCacheIndex.get(tenantId);
            if (keys == null || keys.isEmpty()) {
                return;
            }
            keys.stream()
                .filter(k -> matches(policyModule, k.getPolicyModule()))
                .filter(k -> matches(policyFunction, k.getPolicyFunction()))
                .filter(k -> hash == null || k.hashCode() == hash)
                .forEach(k -> removeTrackedKey(k, tenantId));
        } catch (Exception e) {
            LOG.warnf(e, "解析 Redis 缓存失效消息失败: %s", payload);
        }
    }

    private boolean matches(String pattern, String value) {
        if (pattern == null) {
            return true;
        }
        return pattern.equals(value);
    }

    private void publishInvalidation(PolicyCacheKey key) {
        if (key == null) {
            return;
        }
        publishInvalidation(key.getTenantId(), key.getPolicyModule(), key.getPolicyFunction(), key.hashCode());
    }

    private void publishInvalidation(String tenantId, String module, String function, Integer hash) {
        if (pubSubCommands == null || tenantId == null) {
            return;
        }
        try {
            JsonObject json = new JsonObject()
                .put("tenantId", normalizeTenant(tenantId))
                .put("policyModule", module)
                .put("policyFunction", function);
            if (hash != null) {
                json.put("hash", hash);
            }
            pubSubCommands.publish(INVALIDATION_CHANNEL, json.encode());
        } catch (Exception e) {
            LOG.debug("广播缓存失效消息失败", e);
        }
    }

    @PreDestroy
    void shutdownInvalidationChannel() {
        if (redisSubscriber != null) {
            try {
                redisSubscriber.unsubscribe();
            } catch (Exception ignored) {
            }
        }
        if (invalidationExecutor != null) {
            invalidationExecutor.shutdownNow();
        }
    }
}
