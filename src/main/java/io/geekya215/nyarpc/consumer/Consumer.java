package io.geekya215.nyarpc.consumer;

import io.geekya215.nyarpc.UserService;
import io.geekya215.nyarpc.handler.RpcResponseHandler;
import io.geekya215.nyarpc.protocal.*;
import io.geekya215.nyarpc.registry.EtcdRegistry;
import io.geekya215.nyarpc.registry.Instance;
import io.geekya215.nyarpc.registry.Registry;
import io.geekya215.nyarpc.registry.RegistryConfig;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    private static final LoggingHandler CONSUMER_LOGGING_HANDLER = new LoggingHandler();
    private static final ProtocolCodec CONSUMER_PROTOCOL_CODEC = new ProtocolCodec();
    private final @NotNull AtomicLong sequenceGenerator;
    private final @NotNull Map<String, @NotNull List<@NotNull Instance>> services;
    private final @NotNull Registry registry;

    public Consumer(@NotNull ConsumerConfig consumerConfig) {
        this.sequenceGenerator = new AtomicLong(0L);
        // Todo
        // use SPI
        this.registry = new EtcdRegistry(consumerConfig.registryConfig());
        this.services = new ConcurrentHashMap<>();
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
        final ChannelFuture future = bootstrap.group(eventloop)
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
        return future.sync().channel();
    }

    @SuppressWarnings("unchecked")
    public <T> @NotNull T getProxyService(Class<T> serviceClass) {
        ClassLoader classLoader = serviceClass.getClassLoader();
        Class<?>[] interfaces = {serviceClass};
        Object o = Proxy.newProxyInstance(classLoader, interfaces, (proxy, method, args) -> {
            long sequence = nextSequence();
            // Todo
            // use load balance
            final List<@NotNull Instance> instances = services.get(serviceClass.getName());
            final Instance instance = instances.get(0);
            final String[] addr = instance.endpoint().split(":");
            final Channel channel = getChannel(addr[0], Integer.valueOf(addr[1]));

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

            RpcResponseHandler.PROMISE_RESULT.put(sequence, promise);

            promise.await();
            channel.closeFuture().sync();

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

    public static void main(String[] args) {
        final RegistryConfig registryConfig = new RegistryConfig("http://localhost", 2379);
        Consumer consumer = new Consumer(new ConsumerConfig(registryConfig));
        UserService service = consumer.getProxyService(UserService.class);
        String tom = service.greeting("tom");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            int length = rnd.nextInt(5, 10);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length ; j++) {
                sb.append(Character .valueOf((char) ('a' + rnd.nextInt(26))));
            }
            String res = service.greeting(sb.toString());
            System.out.println(res);
        }
    }
}
