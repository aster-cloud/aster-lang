package io.aster.policy.api;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.quarkus.cache.Cache;
import io.smallrye.mutiny.Uni;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 纯JMH运行环境使用的简化版 Quarkus Cache 实现，基于 Caffeine + Mutiny。
 */
final class StandalonePolicyCache implements Cache {

    private final String name;
    private final Object defaultKey = new Object();
    private final AsyncCache<Object, Object> delegate;

    StandalonePolicyCache(String name) {
        this(name, Duration.ofMinutes(30), 4_096);
    }

    StandalonePolicyCache(String name, Duration expireAfterWrite, long maximumSize) {
        this.name = Objects.requireNonNull(name, "cache name");
        Caffeine<Object, Object> builder = Caffeine.newBuilder().recordStats();
        if (expireAfterWrite != null) {
            builder.expireAfterWrite(expireAfterWrite);
        }
        if (maximumSize > 0) {
            builder.maximumSize(maximumSize);
        }
        this.delegate = builder.buildAsync();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getDefaultKey() {
        return defaultKey;
    }

    @Override
    public <K, V> Uni<V> get(K key, Function<K, V> valueLoader) {
        return getAsync(key, k -> Uni.createFrom().item(valueLoader.apply(k)));
    }

    @Override
    public <K, V> Uni<V> getAsync(K key, Function<K, Uni<V>> valueLoader) {
        @SuppressWarnings("unchecked")
        CompletableFuture<V> future = (CompletableFuture<V>) delegate.get(key, (k, executor) -> invokeLoader(valueLoader, k));
        return Uni.createFrom().completionStage(future);
    }

    @SuppressWarnings("unchecked")
    private <K, V> CompletableFuture<V> invokeLoader(Function<K, Uni<V>> loader, Object key) {
        CompletableFuture<V> stage = new CompletableFuture<>();
        try {
            loader.apply((K) key)
                .subscribe().with(stage::complete, stage::completeExceptionally);
        } catch (Throwable throwable) {
            stage.completeExceptionally(throwable);
        }
        return stage;
    }

    @Override
    public Uni<Void> invalidate(Object key) {
        delegate.synchronous().invalidate(key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> invalidateAll() {
        delegate.synchronous().invalidateAll();
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> invalidateIf(Predicate<Object> predicate) {
        delegate.synchronous().asMap().keySet().removeIf(predicate);
        return Uni.createFrom().voidItem();
    }

    @Override
    public <T extends Cache> T as(Class<T> type) {
        throw new IllegalStateException("StandalonePolicyCache 不支持转换为 " + type.getName());
    }
}
