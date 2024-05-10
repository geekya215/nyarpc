package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum Type {
    REQUEST(1),
    RESPONSE(2);

    private static final Map<Byte, Type> map = new HashMap<>();

    static {
        for (final Type type : Type.values()) {
            map.put(type.value, type);
        }
    }

    private final byte value;

    Type(int value) {
        this.value = (byte) value;
    }

    public static @NotNull Type getType(byte value) {
        final Type type = map.get(value);
        if (type == null) {
            throw new IllegalArgumentException("can not find type value: " + value);
        }
        return type;
    }

    public byte getValue() {
        return value;
    }
}
