package io.geekya215.nyarpc.serializer;

public final class SerializerFactory {
    public static Serializer getSerializer(byte serialization) {
        return switch (serialization) {
            case SerializationType.JDK -> new JdkSerializer();
            default -> throw new IllegalArgumentException("unsupported serialization type");
        };
    }
}
