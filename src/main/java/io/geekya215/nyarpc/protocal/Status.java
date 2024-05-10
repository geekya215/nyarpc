package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum Status {
    INITIAL(0),
    SUCCESS(1),
    FAIL(2);

    private static final Map<Byte, Status> map = new HashMap<>();

    static {
        for (final Status status : Status.values()) {
            map.put(status.value, status);
        }
    }

    private final byte value;

    Status(int value) {
        this.value = (byte) value;
    }

    public static @NotNull Status getStatus(byte value) {
        final Status status = map.get(value);
        if (status == null) {
            throw new IllegalArgumentException("can not find status value " + value);
        }
        return status;
    }

    public byte getValue() {
        return value;
    }
}
