package io.geekya215.nyarpc.protocal;

public record Header(short magic, byte type, byte serialization, byte compress,
                     byte status, long sequence, int length) {

    public static class Builder {
        short magic;
        byte type;
        byte serialization;
        byte compress;
        byte status;
        long sequence;
        // Todo
        // use varint instead of fixed length int
        int length;

        public Builder() {
        }

        public Builder(Header header) {
            magic = header.magic;
            type = header.type;
            serialization = header.serialization;
            compress = header.compress;
            status = header.status;
            sequence = header.sequence;
            length = header.length;
        }

        public Builder magic(short magic) {
            this.magic = magic;
            return this;
        }

        public Builder type(byte type) {
            this.type = type;
            return this;
        }

        public Builder serialization(byte serialization) {
            this.serialization = serialization;
            return this;
        }

        public Builder compress(byte compress) {
            this.compress = compress;
            return this;
        }

        public Builder status(byte status) {
            this.status = status;
            return this;
        }

        public Builder sequence(long sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder length(int length) {
            this.length = length;
            return this;
        }

        public Header build() {
            return new Header(magic, type, serialization, compress, status, sequence, length);
        }
    }
}
