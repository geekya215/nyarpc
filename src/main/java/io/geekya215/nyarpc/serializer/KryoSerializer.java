package io.geekya215.nyarpc.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public final class KryoSerializer implements Serializer {
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        final Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        // resolve circular reference
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public byte @NotNull [] serialize(@NotNull Object object) throws IOException {
        try (
                final ByteArrayOutputStream aos = new ByteArrayOutputStream();
                final Output output = new Output(aos);
        ) {
            final Kryo kryo = KRYO_THREAD_LOCAL.get();

            kryo.writeObject(output, object);
            output.flush();

            return aos.toByteArray();
        }
    }

    @Override
    public <T> @NotNull T deserialize(byte @NotNull [] data, @NotNull Class<T> clazz) throws IOException {
        try (
                final ByteArrayInputStream ais = new ByteArrayInputStream(data);
                final Input input = new Input(ais)
        ) {
            final Kryo kryo = KRYO_THREAD_LOCAL.get();

            return kryo.readObject(input, clazz);
        }
    }
}
