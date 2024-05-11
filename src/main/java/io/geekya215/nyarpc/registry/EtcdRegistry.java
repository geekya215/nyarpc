package io.geekya215.nyarpc.registry;

import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import io.geekya215.nyarpc.util.Pair;
import io.geekya215.nyarpc.exception.RegistryException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
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

    private @NotNull String buildServiceKey(@NotNull ServiceMeta serviceMeta) {
        return RPC_NAMESPACE + NAMESPACE_DELIMITER + serviceMeta.serviceName() + "=" + serviceMeta.address().resolve();
    }

    private @NotNull ServiceMeta parseServiceKey(@NotNull String serviceKey) {
        final String[] part = serviceKey.substring(RPC_NAMESPACE.length() + 1).split("=");
        if (part.length != 2) {
            throw new IllegalArgumentException();
        }
        final String serviceName = part[0];
        final Address address = Address.from(part[1]);
        return new ServiceMeta(serviceName, address);
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
                    .map(keyValue -> {
                        final ServiceMeta serviceMeta = parseServiceKey(keyValue.getKey().toString());
                        final int weight = Integer.parseInt(keyValue.getValue().toString());
                        final Instance instance = new Instance(serviceMeta.address(), weight);
                        final String serviceName = serviceMeta.serviceName();
                        return new Pair<>(serviceName, instance);
                    })
                    .collect(Collectors.groupingBy(
                            Pair::fst,
                            Collectors.mapping(Pair::snd, Collectors.toList())
                    ));

            return result;
        } catch (ExecutionException | InterruptedException e) {
            throw new RegistryException("discovery service failed, cause: ", e);
        }
    }

    @Override
    public void watch(@NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> instances) {
        // Todo
        // consider move to class field?
        final Watch watchClient = client.getWatchClient();
        final WatchOption watchOption = WatchOption.builder().isPrefix(true).withPrevKV(true).build();

        watchClient.watch(ByteSequence.from((RPC_NAMESPACE + NAMESPACE_DELIMITER).getBytes()), watchOption, watchResponse -> {
            for (final WatchEvent event : watchResponse.getEvents()) {
                final WatchEvent.EventType eventType = event.getEventType();
                final KeyValue keyValue = event.getKeyValue();
                final KeyValue prevKeyValue = event.getPrevKV();

                if (eventType == WatchEvent.EventType.PUT) {
                    if (event.getPrevKV().getKey().isEmpty()) {
                        final String keyString = keyValue.getKey().toString();
                        final String valueString = keyValue.getValue().toString();
                        final ServiceMeta serviceMeta = parseServiceKey(keyString);
                        log.info("discovery new service {}", serviceMeta);

                        final String serviceName = serviceMeta.serviceName();
                        final Instance instance = new Instance(serviceMeta.address(), Integer.parseInt(valueString));

                        instances.putIfAbsent(serviceName, new ArrayList<>());
                        instances.get(serviceName).add(instance);

                    }
                } else if (eventType == WatchEvent.EventType.DELETE) {
                    final String prevKeyString = prevKeyValue.getKey().toString();
                    final String prevValueString = prevKeyValue.getValue().toString();

                    final ServiceMeta serviceMeta = parseServiceKey(prevKeyString);
                    log.info("remove unregistered service {}", serviceMeta);

                    final String serviceName = serviceMeta.serviceName();
                    final Instance instance = new Instance(serviceMeta.address(), Integer.parseInt(prevValueString));

                    instances.get(serviceName).remove(instance);
                }
            }
        });
    }

    @Override
    public void close() {
        client.close();
    }
}
