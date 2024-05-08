package io.geekya215.nyarpc.loadbalance;

import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomLoadBalancer implements LoadBalancer {
    private static final ThreadLocalRandom rnd = ThreadLocalRandom.current();

    @Override
    public @NotNull Channel select(@NotNull Class<?> clazz, @NotNull List<@NotNull Channel> channels) {
        return channels.get(rnd.nextInt(channels.size()));
    }
}
