package io.geekya215.nyarpc.registry;

import org.jetbrains.annotations.NotNull;

public record Instance(@NotNull String endpoint, int weight) {
}
