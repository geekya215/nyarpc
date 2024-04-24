package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public record RpcRequest(@NotNull String serviceName, @NotNull String methodName, @NotNull Class<?>[] parameterTypes,
                         @NotNull Object[] args) implements Serializable {

    public static final class Builder {
        String serviceName;
        String methodName;
        Class<?>[] parameterTypes;
        Object[] args;

        public Builder() {
        }

        public Builder serviceName(@NotNull String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder methodName(@NotNull String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder parameterTypes(@NotNull Class<?>[] parameterTypes) {
            this.parameterTypes = parameterTypes;
            return this;
        }

        public Builder args(@NotNull Object[] args) {
            this.args = args;
            return this;
        }

        public RpcRequest build() {
            Objects.requireNonNull(serviceName, "service name must not be null");
            Objects.requireNonNull(methodName, "method name must not be null");
            Objects.requireNonNull(parameterTypes, "parameter types must not be null");
            Objects.requireNonNull(args, "args must not be null");
            return new RpcRequest(serviceName, methodName, parameterTypes, args);
        }
    }
}
