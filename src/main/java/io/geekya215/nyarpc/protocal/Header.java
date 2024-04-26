package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

public record Header(short magic, byte type, byte serializer, byte compress, byte status,
                     long sequence, int length) {

    public static class Builder {
        private short magic;
        private byte type;
        private byte serializer;
        private byte compress;
        private byte status;
        private long sequence;
        private int length;

        public Builder() {
        }

        public @NotNull Builder magic(short magic) {
            this.magic = magic;
            return this;
        }

        public @NotNull Builder type(byte type) {
            this.type = type;
            return this;
        }

        public @NotNull Builder serializer(byte serializer) {
            this.serializer = serializer;
            return this;
        }

        public @NotNull Builder compress(byte compress) {
            this.compress = compress;
            return this;
        }

        public @NotNull Builder status(byte status) {
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
            return new Header(magic, type, serializer, compress, status, sequence, length);
        }
    }
}
