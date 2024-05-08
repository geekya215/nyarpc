package io.geekya215.nyarpc.exception;

public final class RegistryException extends RuntimeException {
    public RegistryException() {
    }

    public RegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
