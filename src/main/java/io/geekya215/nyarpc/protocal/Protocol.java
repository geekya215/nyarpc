package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

public record Protocol<T>(@NotNull Header header, @NotNull T body) {
    public static final short MAGIC = 0x0721;
    public static final int HEADER_LENGTH = 14;
}
