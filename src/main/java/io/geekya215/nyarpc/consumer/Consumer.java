package io.geekya215.nyarpc.consumer;

import io.geekya215.nyarpc.handler.RpcResponseHandler;
import io.geekya215.nyarpc.protocal.*;
import io.geekya215.nyarpc.registry.EtcdRegistry;
import io.geekya215.nyarpc.registry.Instance;
import io.geekya215.nyarpc.registry.Registry;
import io.geekya215.nyarpc.serializer.SerializationType;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private static final LoggingHandler CONSUMER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec CONSUMER_PROTOCOL_CODEC = new ProtocolCodec();

    private final @NotNull AtomicLong sequenceGenerator;
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> services;
    private final @NotNull Map<@NotNull Class<?>, @NotNull List<@NotNull Channel>> channels;
    private final @NotNull Registry registry;

    public Consumer(@NotNull ConsumerConfig consumerConfig) {
        this.sequenceGenerator = new AtomicLong(0L);
        // Todo
        // use SPI
        this.registry = new EtcdRegistry(consumerConfig.registryConfig());

        this.services = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();

        // Todo
        // use init method
        discoveryService();
    }

    private void discoveryService() {
        for (final Map.Entry<String, List<Instance>> entry : registry.discovery().entrySet()) {
            logger.info("discovery {} at {}", entry.getKey(), entry.getValue());
            services.put(entry.getKey(), entry.getValue());
        }
    }

    private @NotNull Channel getChannel(@NotNull String host, int port) throws InterruptedException {
        final Bootstrap bootstrap = new Bootstrap();
        final NioEventLoopGroup eventloop = new NioEventLoopGroup();

        final ChannelFuture future = bootstrap
                .group(eventloop)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        pipeline.addLast(CONSUMER_LOGGING_HANDLER);
                        pipeline.addLast(new ProtocolFrameDecoder());
                        pipeline.addLast(CONSUMER_PROTOCOL_CODEC);
                        pipeline.addLast(new RpcResponseHandler());
                    }
                })
                .connect(host, port);

        // Todo
        // should close eventloop in close future listener
        return future.sync().channel();
    }

    @SuppressWarnings("unchecked")
    public <T> @NotNull T getProxyService(@NotNull Class<T> serviceClass) {
        final ClassLoader classLoader = serviceClass.getClassLoader();
        final Class<?>[] interfaces = {serviceClass};
        final Object o = Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
            final long sequence = nextSequence();

            final List<@NotNull Channel> candidateChannels = channels.computeIfAbsent(serviceClass, clazz -> {
                final List<@NotNull Instance> instances = services.get(clazz.getName());
                final List<@NotNull Channel> tmp = new ArrayList<>();

                for (final Instance instance : instances) {
                    final String[] addr = instance.endpoint().split(":");
                    final Channel channel;

                    try {
                        channel = getChannel(addr[0], Integer.parseInt(addr[1]));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    tmp.add(channel);
                }

                return tmp;
            });

            // Todo
            // use load balance
            final Channel channel = candidateChannels.getFirst();

            final Header.Builder headerBuilder = new Header.Builder();
            final Header header = headerBuilder
                    .magic(Protocol.MAGIC)
                    .type(MessageType.REQUEST)
                    .serializer(SerializationType.JDK)
                    .sequence(sequence)
                    .build();

            final RpcRequest.Builder requestBuilder = new RpcRequest.Builder();
            final RpcRequest rpcRequest = requestBuilder
                    .serviceName(serviceClass.getName())
                    .methodName(method.getName())
                    .returnType(method.getReturnType())
                    .parameterTypes(method.getParameterTypes())
                    .args(args)
                    .build();

            channel.writeAndFlush(new Protocol<>(header, rpcRequest));

            final DefaultPromise<Object> promise = new DefaultPromise<>(channel.eventLoop());

            RpcResponseHandler.PROMISE_RESULTS.put(sequence, promise);

            promise.await();

            if (promise.isSuccess()) {
                return promise.getNow();
            } else {
                throw new RuntimeException(promise.cause());
            }
        });
        return (T) o;
    }

    private long nextSequence() {
        return sequenceGenerator.getAndIncrement();
    }
}
