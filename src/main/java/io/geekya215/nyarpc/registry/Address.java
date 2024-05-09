package io.geekya215.nyarpc.registry;

import org.jetbrains.annotations.NotNull;

// only support IPV4
public record Address(@NotNull String host, int port) {
    public static @NotNull Address from(String s) {
        final String[] part = s.split(":");
        if (part.length != 2) {
            throw new IllegalArgumentException("invalid address format");
        }
        return new Address(part[0], Integer.parseInt(part[1]));
    }

    public String resolve() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return resolve();
    }
}
