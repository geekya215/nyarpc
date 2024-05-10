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

        buf.writeByte(header.type().getValue());
        buf.writeByte(header.serialization().getValue());
        buf.writeByte(header.compress().getValue());
        buf.writeByte(header.status().getValue());

        buf.writeLong(header.sequence());

        final Serializer serializer = SerializerFactory.getSerializer(header.serialization());
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

        final byte typeValue = byteBuf.readByte();
        final byte serialization = byteBuf.readByte();
        final byte compressValue = byteBuf.readByte();
        final byte statusValue = byteBuf.readByte();
        final long sequence = byteBuf.readLong();

        final int length = byteBuf.readInt();
        final byte[] bodyByteArray = new byte[length];
        byteBuf.readBytes(bodyByteArray);

        final Serializer serializer = SerializerFactory.getSerializer(Serialization.getSerializer(serialization));

        final Class<?> dt = switch (Type.getType(typeValue)) {
            case REQUEST -> RpcRequest.class;
            case RESPONSE -> RpcResponse.class;
        };

        final Object body = serializer.deserialize(bodyByteArray, dt);

        final Header header = headerBuilder
                .magic(Protocol.MAGIC)
                .type(Type.getType(typeValue))
                .serializer(Serialization.getSerializer(serialization))
                .compress(Compress.getNoCompress(compressValue))
                .status(Status.getStatus(statusValue))
                .sequence(sequence)
                .length(length)
                .build();

        final Protocol<?> protocol = new Protocol<>(header, body);

        out.add(protocol);
    }
}
