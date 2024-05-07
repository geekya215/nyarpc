package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.protocal.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public final class RpcRequestHandler extends SimpleChannelInboundHandler<Protocol<RpcRequest>> {
    private final @NotNull Map<String, Class<?>> serviceClasses;

    public RpcRequestHandler(@NotNull Map<String, Class<?>> serviceClasses) {
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
                .type(MessageType.RESPONSE)
                .serializer(requestHeader.serializer())
                .compress(requestHeader.compress())
                .sequence(requestHeader.sequence());

        try {
            final Object responseData = handle(request);
            if (responseData == null) {
                responseBuilder.type(RpcResponse.RESPONSE_NULL);
            } else {
                responseBuilder.type(RpcResponse.RESPONSE_VALUE);
            }

            responseHeaderBuilder.status(MessageStatus.SUCCESS);
            responseBuilder.data(responseData);
        } catch (Exception e) {
            e.printStackTrace();
            responseHeaderBuilder.status(MessageStatus.FAIL);

            responseBuilder.type(RpcResponse.RESPONSE_WITH_EXCEPTION);
            responseBuilder.data("RPC failed, cause: " + e);
        }

        final Header responseHeader = responseHeaderBuilder.build();
        final RpcResponse response = responseBuilder.build();

        ctx.writeAndFlush(new Protocol<>(responseHeader, response));
    }

    private @Nullable Object handle(@NotNull RpcRequest request) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<?> clazz = serviceClasses.get(request.serviceName());
        if (clazz == null) {
            throw new ClassNotFoundException(request.serviceName());
        }
        final Method method = clazz.getDeclaredMethod(request.methodName(), request.returnType());
        return method.invoke(clazz.getConstructor().newInstance(), request.args());
    }
}
