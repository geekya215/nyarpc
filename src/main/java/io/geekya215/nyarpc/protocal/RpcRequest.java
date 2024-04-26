package io.geekya215.nyarpc.protocal;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

public record RpcRequest(@NotNull String serviceName, @NotNull String methodName, @NotNull Class<?> returnType,
                         @NotNull Class<?>[] parameterTypes, @NotNull Object[] args) implements Serializable {

    public static final class Builder {
        private String serviceName;
        private String methodName;
        private Class<?> returnType;
        private Class<?>[] parameterTypes;
        private Object[] args;

        public Builder() {
        }

        public @NotNull Builder serviceName(@NotNull String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public @NotNull Builder methodName(@NotNull String methodName) {
            this.methodName = methodName;
            return this;
        }

        public @NotNull Builder returnType(@NotNull Class<?> returnType) {
            this.returnType = returnType;
            return this;
        }

        public @NotNull Builder parameterTypes(@NotNull Class<?>[] parameterTypes) {
            this.parameterTypes = parameterTypes;
            return this;
        }

        public @NotNull Builder args(@NotNull Object[] args) {
            this.args = args;
            return this;
        }

        public @NotNull RpcRequest build() {
            Objects.requireNonNull(serviceName, "service name must not be null");
            Objects.requireNonNull(methodName, "method name must not be null");
            Objects.requireNonNull(returnType, "return type must not be null");
            Objects.requireNonNull(parameterTypes, "parameter types must not be null");
            Objects.requireNonNull(args, "args must not be null");
            return new RpcRequest(serviceName, methodName, returnType, parameterTypes, args);
        }
    }
}
