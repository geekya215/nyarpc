package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.exception.RpcException;
import io.geekya215.nyarpc.protocal.*;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

public final class RpcRequestHandler extends SimpleChannelInboundHandler<Protocol<RpcRequest>> {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);
    private final @NotNull Map<@NotNull String, @NotNull Class<?>> serviceClasses;

    public RpcRequestHandler(@NotNull Map<@NotNull String, @NotNull Class<?>> serviceClasses) {
        this.serviceClasses = serviceClasses;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Protocol<RpcRequest> protocol) throws Exception {
        final Header requestHeader = protocol.header();
        final RpcRequest request = protocol.body();
        final Header.Builder responseHeaderBuilder = new Header.Builder();
        final RpcResponse.Builder responseBuilder = new RpcResponse.Builder();

        responseHeaderBuilder
                .magic(Protocol.MAGIC)
                .type(Type.RESPONSE)
                .serializer(requestHeader.serialization())
                .compress(requestHeader.compress())
                .sequence(requestHeader.sequence());

        try {
            // inject trace id
            final UUID uuid = UUID.randomUUID();
            MDC.put("traceId", uuid.toString());

            logger.info("Service [{}] called from {}", request.serviceName(), ctx.channel().remoteAddress());
            final Object responseData = handle(request);
            if (responseData == null) {
                responseBuilder.type(RpcResponse.RESPONSE_NULL);
            } else {
                responseBuilder.type(RpcResponse.RESPONSE_VALUE);
            }

            responseHeaderBuilder.status(Status.SUCCESS);
            responseBuilder.data(responseData);
            logger.info("Invoke successful for [{}] - [{}]", request.serviceName(), request.methodName());
        } catch (Exception e) {
            logger.error("Invoke failed for [{}] - [{}], caused: ", request.serviceName(), request.methodName(), e);
            responseHeaderBuilder.status(Status.FAIL);

            responseBuilder.type(RpcResponse.RESPONSE_WITH_EXCEPTION);
            responseBuilder.data(new RpcException("Rpc failed, cause " + e.getCause()));
        }

        final Header responseHeader = responseHeaderBuilder.build();
        final RpcResponse response = responseBuilder.build();

        ctx.writeAndFlush(new Protocol<>(responseHeader, response))
                .addListener((ChannelFutureListener) channelFuture -> MDC.remove("traceId"));
    }

    private @Nullable Object handle(@NotNull RpcRequest request)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<?> clazz = serviceClasses.get(request.serviceName());
        if (clazz == null) {
            throw new ClassNotFoundException(request.serviceName());
        }
        final Method method = clazz.getDeclaredMethod(request.methodName(), request.parameterTypes());
        logger.info("Invoke method [{}], args: {}", request.methodName(), request.args());
        return method.invoke(clazz.getConstructor().newInstance(), request.args());
    }
}
