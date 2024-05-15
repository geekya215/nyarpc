package io.geekya215.nyarpc.consumer;

import dev.failsafe.*;
import io.geekya215.nyarpc.annotation.Retryable;
import io.geekya215.nyarpc.annotation.RpcReference;
import io.geekya215.nyarpc.exception.RpcException;
import io.geekya215.nyarpc.handler.RpcResponseHandler;
import io.geekya215.nyarpc.loadbalance.LoadBalancer;
import io.geekya215.nyarpc.loadbalance.RoundRobinLoadBalancer;
import io.geekya215.nyarpc.protocal.*;
import io.geekya215.nyarpc.registry.Address;
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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);

    private static final LoggingHandler CONSUMER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec CONSUMER_PROTOCOL_CODEC = new ProtocolCodec();

    private final @NotNull AtomicLong sequenceGenerator;
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull Instance>> instances;
    private final @NotNull Map<@NotNull Instance, @NotNull Channel> channels;
    private final @NotNull Map<@NotNull Instance, @NotNull CircuitBreaker<Object>> circuitBreakers;
    private final @NotNull Registry registry;
    private final @NotNull LoadBalancer loadBalancer;

    public Consumer(@NotNull ConsumerConfig config) {
        this.sequenceGenerator = new AtomicLong(0L);

        this.registry = ServiceLoader.load(Registry.class).findFirst().orElse(new EtcdRegistry());
        this.registry.init(config.registryConfig());

        this.loadBalancer = ServiceLoader.load(LoadBalancer.class).findFirst().orElse(new RoundRobinLoadBalancer());

        this.instances = new ConcurrentHashMap<>();
        this.channels = new ConcurrentHashMap<>();
        this.circuitBreakers = new ConcurrentHashMap<>();

        discoveryService();
        registry.watch(instances);
    }

    private void discoveryService() {
        for (final Map.Entry<String, List<Instance>> entry : registry.discovery().entrySet()) {
            logger.info("Discovery {} at {}", entry.getKey(), entry.getValue());
            instances.put(entry.getKey(), entry.getValue());
        }
    }

    private @NotNull Channel getChannel(@NotNull String host, int port) throws InterruptedException {
        final Bootstrap bootstrap = new Bootstrap();
        final NioEventLoopGroup eventloop = new NioEventLoopGroup();

        try {
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

            logger.info("Connect to {}:{}", host, port);

            final Channel channel = future.sync().channel();
            channel.closeFuture().addListener((ChannelFutureListener) channelFuture -> {
                eventloop.shutdownGracefully();
            });

            return channel;
        } catch (InterruptedException e) {
            eventloop.shutdownGracefully();
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> @NotNull T getProxyService(@NotNull Class<T> serviceClass) {
        final ClassLoader classLoader = serviceClass.getClassLoader();
        final Class<?>[] interfaces = {serviceClass};
        final Object o = Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {

            final RpcReference rpcReference = serviceClass.getAnnotation(RpcReference.class);
            if (rpcReference == null) {
                throw new RpcException("Can not proxy class: " + serviceClass.getName() + " without @RpcReference");
            }

            final List<@NotNull Instance> candidateInstances = instances.get(serviceClass.getName());
            if (candidateInstances == null || candidateInstances.isEmpty()) {
                throw new RuntimeException("No available service " + serviceClass.getName());
            }

            final Instance instance = loadBalancer.select(serviceClass, candidateInstances);
            final Retryable retry = serviceClass.getAnnotation(Retryable.class);
            final io.geekya215.nyarpc.annotation.CircuitBreaker circuit = serviceClass.getAnnotation(io.geekya215.nyarpc.annotation.CircuitBreaker.class);

            final List<@NotNull Policy<Object>> policies = new ArrayList<>();

            if (retry != null) {
                final RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                        .handle(retry.retryFor())
                        .withMaxRetries(retry.maxRetries())
                        .withDelay(Duration.of(retry.delay(), retry.unit()))
                        .onRetry(e -> logger.warn("Rpc call failed, retry {} times(s), caused: {}", e.getAttemptCount(), e.getLastException().getMessage()))
                        .onFailure(e -> logger.error("Retry failed, caused: ", e.getException()))
                        .build();

                policies.add(retryPolicy);
            }

            if (circuit != null) {
                final CircuitBreaker<Object> cb = circuitBreakers.computeIfAbsent(
                        instance,
                        inst -> CircuitBreaker.builder()
                                .withFailureThreshold(circuit.failureThreshold())
                                .withSuccessThreshold(circuit.successThreshold())
                                .withDelay(Duration.of(circuit.delay(), circuit.unit()))
                                .onClose(e -> logger.info("Circuit breaker for #{} is closed", inst))
                                .onHalfOpen(e -> logger.info("Circuit breaker for #{} is half opened", inst))
                                .onOpen(e -> logger.warn("Circuit breaker for #{} is opened", inst))
                                .build());

                policies.add(cb);
            }

            final FailsafeExecutor<Object> failsafe = policies.isEmpty() ? Failsafe.none() : Failsafe.with(policies);

            final Object result = failsafe.get(() -> doProxy(instance, rpcReference, serviceClass, method, args));

            return result;
        });
        return (T) o;
    }

    private @Nullable Object doProxy(
            @NotNull Instance instance,
            @NotNull RpcReference rpcReference,
            @NotNull Class<?> serviceClass,
            @NotNull Method method,
            @Nullable Object @Nullable [] args
    ) throws InterruptedException {
        final long sequence = nextSequence();

        final Channel channel = channels.computeIfAbsent(instance, inst -> {
            try {
                return addChannel(inst);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        final Header.Builder headerBuilder = new Header.Builder();
        final Header header = headerBuilder
                .magic(Protocol.MAGIC)
                .type(Type.REQUEST)
                .serializer(rpcReference.serializer())
                .compress(rpcReference.compress())
                .status(Status.INITIAL)
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

        promise.await(rpcReference.timeout(), rpcReference.unit());

        if (promise.isSuccess()) {
            return promise.getNow();
        } else {
            if (!promise.isDone()) {
                promise.cancel(true);
                throw new RuntimeException("Rpc call timeout");
            } else {
                throw new RuntimeException(promise.cause());
            }
        }
    }

    private @NotNull Channel addChannel(@NotNull Instance inst) throws InterruptedException {
        final Address address = inst.address();

        final Channel ch = getChannel(address.host(), address.port());
        ch.closeFuture().addListener(
                (ChannelFutureListener) channelFuture -> logger.info("Remove closed channel {}", channels.remove(inst)));
        return ch;
    }

    private long nextSequence() {
        return sequenceGenerator.getAndIncrement();
    }
}
