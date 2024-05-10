package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum Serialization {
    JDK(1),
    KRYO(2);

    private static final Map<Byte, Serialization> map = new HashMap<>();

    static {
        for (final Serialization serialization : Serialization.values()) {
            map.put(serialization.value, serialization);
        }
    }

    private final byte value;

    Serialization(int value) {
        this.value = (byte) value;
    }

    public static @NotNull Serialization getSerializer(byte value) {
        final Serialization serialization = map.get(value);
        if (serialization == null) {
            throw new IllegalArgumentException("can not find serializer value: " + value);
        }
        return serialization;
    }

    public byte getValue() {
        return value;
    }
}
