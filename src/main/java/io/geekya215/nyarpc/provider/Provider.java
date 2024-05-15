package io.geekya215.nyarpc.provider;

import io.geekya215.nyarpc.annotation.RpcService;
import io.geekya215.nyarpc.handler.ExceptionHandler;
import io.geekya215.nyarpc.handler.IdleHandler;
import io.geekya215.nyarpc.handler.RpcRequestHandler;
import io.geekya215.nyarpc.protocal.ProtocolCodec;
import io.geekya215.nyarpc.protocal.ProtocolFrameDecoder;
import io.geekya215.nyarpc.registry.Address;
import io.geekya215.nyarpc.registry.EtcdRegistry;
import io.geekya215.nyarpc.registry.Registry;
import io.geekya215.nyarpc.registry.ServiceMeta;
import io.geekya215.nyarpc.util.ClassUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class Provider implements Closeable {
    static final int DEFAULT_WEIGHT = 1;
    static final int DEFAULT_READ_IDLE_TIMEOUT = 10000;
    static final int DEFAULT_SERVICE_HEARTBEAT_INTERVAL = 10000;
    static final int DEFAULT_WRITE_LIMIT = 1024 * 1024; // 1 MB
    static final int DEFAULT_READ_LIMIT = 1024 * 1024; // 1 MB
    static final int DEFAULT_TRAFFIC_CHECK_INTERVAL = 1000; // 1000 ms

    private static final Logger logger = LoggerFactory.getLogger(Provider.class);
    private static final LoggingHandler PROVIDER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec PROVIDER_PROTOCOL_CODEC = new ProtocolCodec();

    private final @NotNull Map<@NotNull String, @NotNull Class<?>> serviceClasses;
    private final @NotNull ProviderConfig config;
    private final @NotNull Registry registry;
    private final @NotNull ScheduledExecutorService serviceKeepAlive;

    public Provider(@NotNull ProviderConfig config) {
        this.config = config;

        this.registry = ServiceLoader.load(Registry.class).findFirst().orElse(new EtcdRegistry());
        this.registry.init(config.registryConfig());

        this.serviceClasses = new ConcurrentHashMap<>();
        this.serviceKeepAlive = Executors.newScheduledThreadPool(2);
    }

    public void registerRpcService() {
        try {
            final List<Class<?>> classes = ClassUtil.scanClassesWithAnnotation(config.scanPath(), RpcService.class);
            for (final Class<?> clazz : classes) {
                final RpcService annotation = clazz.getAnnotation(RpcService.class);
                final Class<?> serviceClass = annotation.serviceClass();
                final ServiceMeta serviceMeta = new ServiceMeta(serviceClass.getName(), new Address(config.host(), config.port()));

                registry.register(serviceMeta, config.weight());
                logger.info("register service {}", serviceClass.getName());

                serviceClasses.putIfAbsent(serviceClass.getName(), clazz);
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.error("register local service failed, cause: {}", e.getMessage());
        }
    }

    public void start() throws InterruptedException {
        // initial service
        registerRpcService();

        // heartbeat
        serviceKeepAlive.scheduleAtFixedRate(
                this::registerRpcService,
                config.serviceHeartbeatInterval(),
                config.serviceHeartbeatInterval(),
                TimeUnit.MILLISECONDS);

        // bind core
        final AffinityThreadFactory affinity =
                new AffinityThreadFactory("affinity", AffinityStrategies.DIFFERENT_CORE, AffinityStrategies.DIFFERENT_SOCKET);

        final ServerBootstrap bootstrap = new ServerBootstrap();
        final EpollEventLoopGroup boss = new EpollEventLoopGroup();
        final EpollEventLoopGroup worker = new EpollEventLoopGroup(affinity);

        try {
            final ChannelFuture bindFuture = bootstrap
                    .group(boss, worker)
                    .channel(EpollServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new ChannelTrafficShapingHandler(config.writeLimit(), config.readLimit(), config.trafficCheckInterval()));
                            pipeline.addLast(PROVIDER_LOGGING_HANDLER);
                            pipeline.addLast(new ProtocolFrameDecoder());
                            pipeline.addLast(PROVIDER_PROTOCOL_CODEC);
                            pipeline.addLast(new IdleStateHandler(config.readIdleTimeout(), 0, 0, TimeUnit.MILLISECONDS));
                            pipeline.addLast(new RpcRequestHandler(serviceClasses));
                            pipeline.addLast(new IdleHandler());
                            pipeline.addLast(new ExceptionHandler());
                        }
                    })
                    .bind(config.port());

            bindFuture.sync();
            logger.info("provider start at port: {}", config.port());
            bindFuture.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    @Override
    public void close() throws IOException {
        // stop heartbeat
        serviceKeepAlive.shutdownNow();

        // unregister all services
        for (final String serviceName : serviceClasses.keySet()) {
            registry.unregister(new ServiceMeta(serviceName, new Address(config.host(), config.port())));
        }
    }
}
