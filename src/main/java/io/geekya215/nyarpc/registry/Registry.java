package io.geekya215.nyarpc.registry;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

public interface Registry extends Closeable {
    String RPC_NAMESPACE = "rpc";

    void init(@NotNull RegistryConfig registryConfig);

    void register(@NotNull ServiceMeta serviceMeta);

    void unregister(@NotNull ServiceMeta serviceMeta);

    @NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> discovery();
}
