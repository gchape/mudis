package io.mudis.mudisserver.codec;

import io.mudis.mudisserver.model.Message;
import io.mudis.mudisshared.model.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Codec for decoding client messages and encoding server responses.
 * Protocol: [operation_ordinal:int][args_length:int][args:bytes]
 */
public class ServerCodec extends ByteToMessageCodec<String> {
    private static final Logger Log = LoggerFactory.getLogger(ServerCodec.class);
    private static final int MIN_HEADER_SIZE = 8; // 4 bytes for operation + 4 bytes for length

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MIN_HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        try {
            Operation op = readOperation(in);
            String args = readArguments(in);

            if (args == null) {
                in.resetReaderIndex();
                return;
            }

            Message message = Message.of(op, args);
            out.add(message);

        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.error("Error decoding message", e);
            in.resetReaderIndex();
            throw e;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Codec error", cause);
        ctx.close();
    }

    private Operation readOperation(ByteBuf in) {
        int ordinal = in.readInt();
        return Operation.values()[ordinal];
    }

    private String readArguments(ByteBuf in) {
        int size = in.readInt();

        if (in.readableBytes() < size) {
            return null;
        }

        if (size == 0) {
            return "";
        }

        byte[] argsBytes = new byte[size];
        in.readBytes(argsBytes);
        return new String(argsBytes, StandardCharsets.UTF_8);
    }
}
