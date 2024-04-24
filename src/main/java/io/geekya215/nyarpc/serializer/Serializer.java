package io.geekya215.nyarpc.serializer;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface Serializer {
    byte @NotNull [] serialize(@NotNull Object object) throws IOException;

    <T> @NotNull T deserialize(byte @NotNull [] data, @NotNull Class<T> clazz) throws IOException, ClassNotFoundException;
}
