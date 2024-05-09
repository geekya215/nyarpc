package io.geekya215.nyarpc.consumer;

import io.geekya215.nyarpc.annotation.RpcReference;
import io.geekya215.nyarpc.exception.RpcException;
import io.geekya215.nyarpc.handler.RpcResponseHandler;
import io.geekya215.nyarpc.loadbalance.LoadBalancer;
import io.geekya215.nyarpc.loadbalance.RoundRobinLoadBalancer;
import io.geekya215.nyarpc.protocal.*;
import io.geekya215.nyarpc.registry.EtcdRegistry;
import io.geekya215.nyarpc.registry.Instance;
import io.geekya215.nyarpc.registry.Registry;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultPromise;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private static final LoggingHandler CONSUMER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec CONSUMER_PROTOCOL_CODEC = new ProtocolCodec();

    private final @NotNull AtomicLong sequenceGenerator;
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> instances;
    private final @NotNull Map<@NotNull Instance, @NotNull Channel> channels;
    private final @NotNull Registry registry;
    private final @NotNull LoadBalancer loadBalancer;

    public Consumer(@NotNull ConsumerConfig config) {
        this.sequenceGenerator = new AtomicLong(0L);

        this.registry = ServiceLoader.load(Registry.class).findFirst().orElse(new EtcdRegistry());
        this.registry.init(config.registryConfig());

        this.loadBalancer = ServiceLoader.load(LoadBalancer.class).findFirst().orElse(new RoundRobinLoadBalancer());

        this.instances = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();

        discoveryService();
    }

    private void discoveryService() {
        for (final Map.Entry<String, List<Instance>> entry : registry.discovery().entrySet()) {
            logger.info("discovery {} at {}", entry.getKey(), entry.getValue());
            instances.put(entry.getKey(), entry.getValue());
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

        logger.info("connect to {}:{}", host, port);

        Channel channel = future.sync().channel();
        channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
            eventloop.shutdownGracefully();
        });

        return channel;
    }

    @SuppressWarnings("unchecked")
    public <T> @NotNull T getProxyService(@NotNull Class<T> serviceClass) {
        final ClassLoader classLoader = serviceClass.getClassLoader();
        final Class<?>[] interfaces = {serviceClass};
        final Object o = Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
            final long sequence = nextSequence();

            final List<@NotNull Instance> candidateInstances = instances.get(serviceClass.getName());
            if (candidateInstances == null) {
                throw new RuntimeException("not available service " + serviceClass.getName());
            }

            final Instance instance = loadBalancer.select(serviceClass, candidateInstances);

            final Channel channel = channels.computeIfAbsent(instance, inst -> {
                final String[] addr = inst.endpoint().split(":");
                try {
                    final Channel ch = getChannel(addr[0], Integer.parseInt(addr[1]));
                    ch.closeFuture().addListener(
                            (ChannelFutureListener) channelFuture -> logger.info("remove idle channel {}", channels.remove(inst)));
                    return ch;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            final RpcReference annotation = serviceClass.getAnnotation(RpcReference.class);
            if (annotation == null) {
                throw new RpcException("Can not proxy class: " + serviceClass.getName() + " without @RpcReference");
            }

            final Header.Builder headerBuilder = new Header.Builder();
            final Header header = headerBuilder
                    .magic(Protocol.MAGIC)
                    .type(MessageType.REQUEST)
                    .serializer(annotation.serializer())
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

            final DefaultChannelPromise defaultChannelPromise = new DefaultChannelPromise(channel);

            channel.writeAndFlush(new Protocol<>(header, rpcRequest), defaultChannelPromise);

            defaultChannelPromise.sync();

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
