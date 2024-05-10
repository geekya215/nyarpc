package io.geekya215.nyarpc.serializer;

import io.geekya215.nyarpc.protocal.Serialization;

public final class SerializerFactory {
    public static Serializer getSerializer(Serialization serialization) {
        return switch (serialization) {
            case JDK -> new JdkSerializer();
            case KRYO -> new KryoSerializer();
        };
    }
}
