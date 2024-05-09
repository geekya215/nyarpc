package io.geekya215.nyarpc.loadbalance;

import io.geekya215.nyarpc.registry.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RoundRobinLoadBalancer implements LoadBalancer {
    private final @NotNull Map<@NotNull Class<?>, @NotNull AtomicLong> currentIndexes;

    public RoundRobinLoadBalancer() {
        this.currentIndexes = new ConcurrentHashMap<>();
    }

    @Override
    public @NotNull Instance select(@NotNull Class<?> clazz, @NotNull List<@NotNull Instance> instances) {
        final AtomicLong idx = currentIndexes.computeIfAbsent(clazz, a -> new AtomicLong());
        return instances.get((int) (idx.getAndIncrement() % instances.size()));
    }
}
