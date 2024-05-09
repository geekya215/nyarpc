package io.geekya215.nyarpc.loadbalance;

import io.geekya215.nyarpc.registry.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomLoadBalancer implements LoadBalancer {
    private static final ThreadLocalRandom rnd = ThreadLocalRandom.current();

    @Override
    public @NotNull Instance select(@NotNull Class<?> clazz, @NotNull List<@NotNull Instance> instances) {
        return instances.get(rnd.nextInt(instances.size()));
    }
}
