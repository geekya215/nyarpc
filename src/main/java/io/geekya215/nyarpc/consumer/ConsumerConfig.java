package io.geekya215.nyarpc.consumer;

import io.geekya215.nyarpc.registry.RegistryConfig;
import org.jetbrains.annotations.NotNull;

public record ConsumerConfig(@NotNull RegistryConfig registryConfig) {
}
