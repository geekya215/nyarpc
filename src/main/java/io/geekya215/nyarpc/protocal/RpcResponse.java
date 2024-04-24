package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record RpcResponse(byte type, @Nullable Object data) implements Serializable {
    public static final byte RESPONSE_NULL = 0;
    public static final byte RESPONSE_VALUE = 1;
    public static final byte RESPONSE_WITH_EXCEPTION = 2;

    public static final class Builder {
        byte type;
        Object data;

        public Builder type(byte type) {
            this.type = type;
            return this;
        }

        public Builder data(@NotNull Object data) {
            this.data = data;
            return this;
        }

        public @NotNull RpcResponse build() {
            return new RpcResponse(type, data);
        }
    }
}
