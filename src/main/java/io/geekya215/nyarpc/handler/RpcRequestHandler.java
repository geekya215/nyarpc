package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.exception.RpcException;
import io.geekya215.nyarpc.protocal.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public final class RpcRequestHandler extends SimpleChannelInboundHandler<Protocol<RpcRequest>> {
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
            final Object responseData = handle(request);
            if (responseData == null) {
                responseBuilder.type(RpcResponse.RESPONSE_NULL);
            } else {
                responseBuilder.type(RpcResponse.RESPONSE_VALUE);
            }

            responseHeaderBuilder.status(Status.SUCCESS);
            responseBuilder.data(responseData);
        } catch (Exception e) {
            responseHeaderBuilder.status(Status.FAIL);

            responseBuilder.type(RpcResponse.RESPONSE_WITH_EXCEPTION);
            responseBuilder.data(new RpcException("Rpc failed, cause " + e.getCause().toString()));
        }

        final Header responseHeader = responseHeaderBuilder.build();
        final RpcResponse response = responseBuilder.build();

        ctx.writeAndFlush(new Protocol<>(responseHeader, response));
    }

    private @Nullable Object handle(@NotNull RpcRequest request)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<?> clazz = serviceClasses.get(request.serviceName());
        if (clazz == null) {
            throw new ClassNotFoundException(request.serviceName());
        }
        final Method method = clazz.getDeclaredMethod(request.methodName(), request.parameterTypes());
        return method.invoke(clazz.getConstructor().newInstance(), request.args());
    }
}
