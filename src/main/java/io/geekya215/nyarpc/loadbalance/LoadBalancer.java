package io.geekya215.nyarpc.loadbalance;

import io.geekya215.nyarpc.registry.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface LoadBalancer
        permits ConsistentHashLoadBalancer, RandomLoadBalancer, RoundRobinLoadBalancer, WeightBasedLoadBalancer {
    @NotNull Instance select(@NotNull Class<?> clazz, @NotNull List<@NotNull Instance> instances);
}
