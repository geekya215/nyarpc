package io.geekya215.nyarpc.registry;

import org.jetbrains.annotations.NotNull;

public record RegistryConfig(@NotNull String host, int port) {
}
