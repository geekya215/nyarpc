package io.geekya215.nyarpc.serializer;

public final class SerializerFactory {
    public static Serializer getSerializer(byte type) {
        return switch (type) {
            case SerializationType.JDK -> new JdkSerializer();
            case SerializationType.KRYO -> new KryoSerializer();
            default -> throw new IllegalArgumentException("unsupported serializer type: " + type);
        };
    }
}
