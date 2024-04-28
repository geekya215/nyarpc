package io.geekya215.nyarpc.protocal;

import io.geekya215.nyarpc.serializer.Serializer;
import io.geekya215.nyarpc.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.List;

@ChannelHandler.Sharable
public final class ProtocolCodec extends MessageToMessageCodec<ByteBuf, Protocol<?>> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Protocol<?> protocol, List<Object> out) throws Exception {
        final ByteBuf buf = ctx.alloc().buffer();
        final Header header = protocol.header();
        final Object body = protocol.body();

        buf.writeShort(header.magic());

        buf.writeByte(header.type());
        buf.writeByte(header.serializer());
        buf.writeByte(header.compress());
        buf.writeByte(header.status());

        buf.writeLong(header.sequence());

        final Serializer serializer = SerializerFactory.getSerializer(header.serializer());
        final byte[] bodyByteArray = serializer.serialize(body);
        buf.writeInt(bodyByteArray.length);
        buf.writeBytes(bodyByteArray);

        out.add(buf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        final short magic = byteBuf.readShort();
        if (magic != Protocol.MAGIC) {
            throw new IllegalAccessException("invalid magic number: " + magic);
        }

        final Header.Builder headerBuilder = new Header.Builder();

        final byte type = byteBuf.readByte();
        final byte serialization = byteBuf.readByte();
        final byte compress = byteBuf.readByte();
        final byte status = byteBuf.readByte();
        final long sequence = byteBuf.readLong();

        final int length = byteBuf.readInt();
        final byte[] bodyByteArray = new byte[length];
        byteBuf.readBytes(bodyByteArray);

        final Serializer serializer = SerializerFactory.getSerializer(serialization);

        final Class<?> dt = switch (type) {
            case MessageType.REQUEST -> RpcRequest.class;
            case MessageType.RESPONSE -> RpcResponse.class;
            default -> throw new IllegalArgumentException("unsupported protocol type: " + type);
        };

        final Object body = serializer.deserialize(bodyByteArray, dt);

        final Header header = headerBuilder
                .magic(Protocol.MAGIC)
                .type(type)
                .serializer(serialization)
                .compress(compress)
                .status(status)
                .sequence(sequence)
                .length(length)
                .build();

        final Protocol<?> protocol = new Protocol<>(header, body);

        out.add(protocol);
    }
}
