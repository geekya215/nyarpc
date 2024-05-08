package io.geekya215.nyarpc.loadbalance;

import io.netty.channel.Channel;
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
    public @NotNull Channel select(@NotNull Class<?> clazz, @NotNull List<@NotNull Channel> channels) {
        final AtomicLong idx = currentIndexes.computeIfAbsent(clazz, a -> new AtomicLong());
        return channels.get((int) (idx.getAndIncrement() % channels.size()));
    }
}
