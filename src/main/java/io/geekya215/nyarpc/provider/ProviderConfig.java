package io.geekya215.nyarpc.provider;

import io.geekya215.nyarpc.registry.RegistryConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record ProviderConfig(
        @NotNull RegistryConfig registryConfig,
        @NotNull String scanPath,
        @NotNull String host,
        int port,
        int weight,
        int readIdleTimeout, // unit: ms
        int serviceHeartbeatInterval, // unit: ms
        int writeLimit, // unit: byte
        int readLimit, // unit: byte
        int trafficCheckInterval // unit: ms
) {
    public static final class ProviderConfigBuilder {
        private RegistryConfig registryConfig;
        private String scanPath;
        private String host;
        private int port;
        private int weight = Provider.DEFAULT_WEIGHT;
        private int readIdleTimeout = Provider.DEFAULT_READ_IDLE_TIMEOUT;
        private int serviceHeartbeatInterval = Provider.DEFAULT_SERVICE_HEARTBEAT_INTERVAL;
        private int writeLimit = Provider.DEFAULT_WRITE_LIMIT;
        private int readLimit = Provider.DEFAULT_READ_LIMIT;
        private int trafficCheckInterval = Provider.DEFAULT_TRAFFIC_CHECK_INTERVAL;

        private ProviderConfigBuilder() {
        }

        public static ProviderConfigBuilder builder() {
            return new ProviderConfigBuilder();
        }

        public ProviderConfigBuilder registryConfig(@NotNull RegistryConfig registryConfig) {
            this.registryConfig = registryConfig;
            return this;
        }

        public ProviderConfigBuilder scanPath(@NotNull String scanPath) {
            this.scanPath = scanPath;
            return this;
        }

        public ProviderConfigBuilder host(@NotNull String host) {
            this.host = host;
            return this;
        }

        public ProviderConfigBuilder port(int port) {
            this.port = port;
            return this;
        }

        public ProviderConfigBuilder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public ProviderConfigBuilder readIdleTimeout(int readIdleTimeout) {
            this.readIdleTimeout = readIdleTimeout;
            return this;
        }

        public ProviderConfigBuilder serviceHeartbeatInterval(int serviceHeartbeatInterval) {
            this.serviceHeartbeatInterval = serviceHeartbeatInterval;
            return this;
        }

        public ProviderConfigBuilder writeLimit(int writeLimit) {
            this.writeLimit = writeLimit;
            return this;
        }

        public ProviderConfigBuilder readLimit(int readLimit) {
            this.readLimit = readLimit;
            return this;
        }

        public ProviderConfigBuilder trafficCheckInterval(int trafficCheckInterval) {
            this.trafficCheckInterval = trafficCheckInterval;
            return this;
        }

        public ProviderConfig build() {
            Objects.requireNonNull(registryConfig, "registry config con not be null");
            Objects.requireNonNull(scanPath, "scan path con not be null");
            Objects.requireNonNull(host, "host con not be null");
            return new ProviderConfig(
                    registryConfig,
                    scanPath,
                    host,
                    port,
                    weight,
                    readIdleTimeout,
                    serviceHeartbeatInterval,
                    writeLimit,
                    readLimit,
                    trafficCheckInterval);
        }
    }
}
