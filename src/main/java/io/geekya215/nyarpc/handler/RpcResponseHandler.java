package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.protocal.Header;
import io.geekya215.nyarpc.protocal.Protocol;
import io.geekya215.nyarpc.protocal.RpcResponse;
import io.geekya215.nyarpc.protocal.Status;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.concurrent.Promise;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RpcResponseHandler extends SimpleChannelInboundHandler<Protocol<RpcResponse>> {
    public static final Map<@NotNull Long, @NotNull Promise<Object>> PROMISE_RESULTS = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(RpcResponseHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Protocol<RpcResponse> protocol) throws Exception {
        final Header header = protocol.header();
        final RpcResponse response = protocol.body();
        final Promise<Object> promise = PROMISE_RESULTS.remove(header.sequence());

        if (promise != null) {
            if (promise.isCancelled()) {
                logger.warn("discard timeout message for request #{}", header.sequence());
            } else {
                if (header.status() == Status.SUCCESS) {
                    promise.setSuccess(response.data());
                } else {
                    promise.setFailure((Throwable) response.data());
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // Todo
        // set exception to promise
        logger.error("failed, caused: ", cause);
    }
}
