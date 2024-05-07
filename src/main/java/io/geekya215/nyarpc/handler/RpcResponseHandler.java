package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.protocal.Header;
import io.geekya215.nyarpc.protocal.MessageStatus;
import io.geekya215.nyarpc.protocal.Protocol;
import io.geekya215.nyarpc.protocal.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpcResponseHandler extends SimpleChannelInboundHandler<Protocol<RpcResponse>> {
    public static final Map<@NotNull Long, @NotNull Promise<Object>> PROMISE_RESULTS = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Protocol<RpcResponse> protocol) throws Exception {
        final Header header = protocol.header();
        final RpcResponse response = protocol.body();
        final Promise<Object> promise = PROMISE_RESULTS.remove(header.sequence());

        if (promise != null) {
            if (header.status() == MessageStatus.SUCCESS) {
                promise.setSuccess(response.data());
            } else {
                promise.setFailure((Throwable) response.data());
            }
        }
    }
}
