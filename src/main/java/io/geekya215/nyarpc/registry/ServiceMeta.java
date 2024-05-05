package io.geekya215.nyarpc.registry;

import org.jetbrains.annotations.NotNull;

public record ServiceMeta(@NotNull String serviceName, @NotNull String address) {
}
