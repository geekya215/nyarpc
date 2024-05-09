package io.geekya215.nyarpc.serializer;

public final class SerializerFactory {
    public static Serializer getSerializer(byte type) {
        return switch (type) {
            case SerializerType.JDK -> new JdkSerializer();
            case SerializerType.KRYO -> new KryoSerializer();
            default -> throw new IllegalArgumentException("Unsupported serializer type: " + type);
        };
    }
}
