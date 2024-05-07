package io.geekya215.nyarpc.provider;

import io.geekya215.nyarpc.registry.RegistryConfig;
import org.jetbrains.annotations.NotNull;

public record ProviderConfig(@NotNull RegistryConfig registryConfig, @NotNull String host, int port) {
}
