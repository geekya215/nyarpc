package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record Header(short magic, Type type, Serialization serialization, Compress compress, Status status,
                     long sequence, int length) {
    public static class Builder {
        private short magic;
        private Type type;
        private Serialization serialization;
        private Compress compress;
        private Status status;
        private long sequence;
        private int length;

        public Builder() {
        }

        public @NotNull Builder magic(short magic) {
            this.magic = magic;
            return this;
        }

        public @NotNull Builder type(@NotNull Type type) {
            this.type = type;
            return this;
        }

        public @NotNull Builder serializer(@NotNull Serialization serialization) {
            this.serialization = serialization;
            return this;
        }

        public @NotNull Builder compress(@NotNull Compress compress) {
            this.compress = compress;
            return this;
        }

        public @NotNull Builder status(@NotNull Status status) {
            this.status = status;
            return this;
        }

        public @NotNull Builder sequence(long sequence) {
            this.sequence = sequence;
            return this;
        }

        public @NotNull Builder length(int length) {
            this.length = length;
            return this;
        }

        public @NotNull Header build() {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(serialization, "serializer must not be null");
            Objects.requireNonNull(compress, "compress must not be null");
            Objects.requireNonNull(status, "status must not be null");
            return new Header(magic, type, serialization, compress, status, sequence, length);
        }
    }
}
