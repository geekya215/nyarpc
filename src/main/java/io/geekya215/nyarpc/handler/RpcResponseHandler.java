package io.geekya215.nyarpc.handler;

import io.geekya215.nyarpc.protocal.Protocol;
import io.geekya215.nyarpc.protocal.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class RpcResponseHandler extends SimpleChannelInboundHandler<Protocol<RpcResponse>> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Protocol<RpcResponse> protocol) throws Exception {
        System.out.println(protocol);
        ctx.close();
    }
}
