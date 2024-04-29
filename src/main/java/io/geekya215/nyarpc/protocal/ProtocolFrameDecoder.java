package io.geekya215.nyarpc.protocal;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public final class ProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {
    public static final int DEFAULT_MAX_FRAME_LENGTH = 65535;

    public ProtocolFrameDecoder() {
        this(DEFAULT_MAX_FRAME_LENGTH,  Protocol.HEADER_LENGTH,4);
    }

    public ProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}
