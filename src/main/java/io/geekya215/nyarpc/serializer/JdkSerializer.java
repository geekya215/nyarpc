package io.geekya215.nyarpc.serializer;

import org.jetbrains.annotations.NotNull;

import java.io.*;

public final class JdkSerializer implements Serializer {
    @Override
    public byte @NotNull [] serialize(@NotNull Object object) throws IOException {
        try (ByteArrayOutputStream aos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(aos)
        ) {
            oos.writeObject(object);
            return aos.toByteArray();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @NotNull T deserialize(byte @NotNull [] data, @NotNull Class<T> clazz) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream ais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(ais)
        ) {
            return (T) ois.readObject();
        }
    }
}
