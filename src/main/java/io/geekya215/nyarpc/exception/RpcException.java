package io.geekya215.nyarpc.exception;

public final class RpcException extends RuntimeException {
    public RpcException() {
    }

    public RpcException(String message) {
        super(message);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
