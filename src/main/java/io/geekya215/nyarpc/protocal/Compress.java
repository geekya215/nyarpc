package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum Compress {
    NO_COMPRESS(0);

    private static final Map<Byte, Compress> map = new HashMap<>();

    static {
        for (final Compress compress : Compress.values()) {
            map.put(compress.value, compress);
        }
    }

    private final byte value;

    Compress(int value) {
        this.value = (byte) value;
    }

    public static @NotNull Compress getNoCompress(byte value) {
        final Compress compress = map.get(value);
        if (compress == null) {
            throw new IllegalArgumentException("can not find compress value: " + value);
        }
        return compress;
    }

    public byte getValue() {
        return value;
    }
}
