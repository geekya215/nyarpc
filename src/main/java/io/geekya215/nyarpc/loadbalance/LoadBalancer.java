package io.geekya215.nyarpc.loadbalance;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface LoadBalancer permits RandomLoadBalancer, RoundRobinLoadBalancer {
    @NotNull
    Channel select(@NotNull Class<?> clazz, @NotNull List<@NotNull Channel> channels);
}
