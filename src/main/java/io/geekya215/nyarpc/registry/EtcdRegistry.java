package io.geekya215.nyarpc.registry;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.geekya215.nyarpc.exception.RegistryException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.etcd.jetcd.ByteSequence.NAMESPACE_DELIMITER;

public final class EtcdRegistry implements Registry {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(3L);
    private static final int DEFAULT_LEASE_TTL = 15; // seconds
    private static final Logger log = LoggerFactory.getLogger(EtcdRegistry.class);

    // lazy instantiate for SPI load
    private Client client;

    public EtcdRegistry() {
    }

    private String buildServiceKey(@NotNull ServiceMeta serviceMeta) {
        return RPC_NAMESPACE + NAMESPACE_DELIMITER + serviceMeta.serviceName() + "=" + serviceMeta.address().resolve();
    }

    @Override
    public void init(@NotNull RegistryConfig registryConfig) {
        this.client = Client.builder()
                .endpoints(registryConfig.host() + ":" + registryConfig.port())
                .connectTimeout(CONNECTION_TIMEOUT)
                .waitForReady(false)
                .build();
    }

    @Override
    public void register(@NotNull ServiceMeta serviceMeta, int weight) {
        try (
                final KV kv = client.getKVClient();
                final Lease leaseClient = client.getLeaseClient()
        ) {
            final long leaseId = leaseClient.grant(DEFAULT_LEASE_TTL).get().getID();
            final PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
            final String key = buildServiceKey(serviceMeta);
            final String value = String.valueOf(weight);

            kv.put(ByteSequence.from(key.getBytes()), ByteSequence.from(value.getBytes()), putOption).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RegistryException("register service failed, cause: ", e);
        }
    }

    @Override
    public void unregister(@NotNull ServiceMeta serviceMeta) {
        try (final KV kv = client.getKVClient()) {
            final String key = buildServiceKey(serviceMeta);
            kv.delete(ByteSequence.from(key.getBytes())).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RegistryException("unregister service failed, cause: ", e);
        }
    }

    public @NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> discovery() {
        try (final KV kv = client.getKVClient()) {
            final String key = RPC_NAMESPACE + NAMESPACE_DELIMITER;
            final GetOption getOption = GetOption.builder().isPrefix(true).build();
            final GetResponse response = kv.get(ByteSequence.from(key.getBytes()), getOption).get();

            final Map<String, List<Instance>> result = response.getKvs().stream()
                    .map(s -> s.getKey().toString())
                    .map(s -> s.substring(RPC_NAMESPACE.length() + 1))
                    .map(s -> s.split("="))
                    .filter(s -> s.length == 2)
                    .collect(Collectors.groupingBy(
                            s -> s[0],
                            Collectors.mapping(s -> new Instance(Address.from(s[1]), 0), Collectors.toList())));

            return result;
        } catch (ExecutionException | InterruptedException e) {
            throw new RegistryException("discovery service failed, cause: ", e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
