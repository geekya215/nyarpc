package io.geekya215.nyarpc.exception;

public class RpcException extends RuntimeException {
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
