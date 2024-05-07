package io.geekya215.nyarpc.provider;

import io.geekya215.nyarpc.annotation.RpcService;
import io.geekya215.nyarpc.handler.RpcRequestHandler;
import io.geekya215.nyarpc.protocal.ProtocolCodec;
import io.geekya215.nyarpc.protocal.ProtocolFrameDecoder;
import io.geekya215.nyarpc.registry.EtcdRegistry;
import io.geekya215.nyarpc.registry.Registry;
import io.geekya215.nyarpc.registry.ServiceMeta;
import io.geekya215.nyarpc.util.ClassUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Provider {
    private static final Logger logger = LoggerFactory.getLogger(Provider.class);

    private static final LoggingHandler PROVIDER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec PROVIDER_PROTOCOL_CODEC = new ProtocolCodec();

    private final @NotNull Map<@NotNull String, @NotNull Class<?>> serviceClasses;
    private final @NotNull ProviderConfig config;
    private final @NotNull Registry registry;

    public Provider(@NotNull ProviderConfig config) {
        this.config = config;
        // Todo
        // use SPI
        this.registry = new EtcdRegistry(config.registryConfig());
        this.serviceClasses = new ConcurrentHashMap<>();
    }

    public void registerRpcService() throws IOException, ClassNotFoundException {
        final List<Class<?>> classes = ClassUtil.scanClassesWithAnnotation("io.geekya215.nyarpc", RpcService.class);
        for (final Class<?> clazz : classes) {
            final RpcService annotation = clazz.getAnnotation(RpcService.class);
            final Class<?> serviceClass = annotation.serviceClass();

            registry.register(new ServiceMeta(serviceClass.getName(), config.host() + ":" + config.port()));
            logger.info("register service {}", serviceClass.getName());
            serviceClasses.put(serviceClass.getName(), clazz);
        }
    }

    public void start() throws IOException, ClassNotFoundException {
        registerRpcService();

        final ServerBootstrap bootstrap = new ServerBootstrap();
        try (
                final NioEventLoopGroup boss = new NioEventLoopGroup();
                final NioEventLoopGroup worker = new NioEventLoopGroup()
        ) {
            final ChannelFuture bindFuture = bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(PROVIDER_LOGGING_HANDLER);
                            pipeline.addLast(new ProtocolFrameDecoder());
                            pipeline.addLast(PROVIDER_PROTOCOL_CODEC);
                            pipeline.addLast(new RpcRequestHandler(serviceClasses));
                        }
                    })
                    .bind(config.port());
            bindFuture.sync();
            logger.info("provider start at port: {}", config.port());
            bindFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
