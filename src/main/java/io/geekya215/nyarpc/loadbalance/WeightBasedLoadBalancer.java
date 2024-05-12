package io.geekya215.nyarpc.loadbalance;

import io.geekya215.nyarpc.registry.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class WeightBasedLoadBalancer implements LoadBalancer {
    private static final ThreadLocalRandom rnd = ThreadLocalRandom.current();

    @Override
    public @NotNull Instance select(@NotNull Class<?> clazz, @NotNull List<@NotNull Instance> instances) {
        final TreeMap<Integer, Instance> nodes = new TreeMap<>();

        int cnt = 0;
        for (final Instance instance : instances) {
            cnt += instance.weight();
            nodes.put(cnt, instance);
        }

        final Map.Entry<Integer, Instance> entry = nodes.ceilingEntry(rnd.nextInt(0, cnt));

        if (entry == null) {
            return nodes.firstEntry().getValue();
        } else {
            return entry.getValue();
        }
    }
}
