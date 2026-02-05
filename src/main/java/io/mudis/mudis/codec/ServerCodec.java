package io.mudis.mudis.codec;

import io.mudis.mudis.model.Message;
import io.mudis.mudis.model.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.mudis.mudis.utils.RequestValidator.validateArgsSize;
import static io.mudis.mudis.utils.RequestValidator.validateOperation;

public class ServerCodec extends ByteToMessageCodec<String> {
    private static final Logger Log = LoggerFactory.getLogger(ServerCodec.class);
    private static final int MIN_HEADER_SIZE = 8;

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        byte[] bytes = msg.getBytes();
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
            String args = readArguments(in, op);

            if (args == null) {
                in.resetReaderIndex();
                return;
            }

            var message = Message.of(op, args);
            out.add(message);

        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.error("Error decoding message", e);
            in.resetReaderIndex();
            throw e;
        }
    }

    private Operation readOperation(ByteBuf in) {
        int ordinal = in.readInt();
        validateOperation(ordinal);

        return Operation.values()[ordinal];
    }

    private String readArguments(ByteBuf in, Operation op) {
        int size = in.readInt();

        validateArgsSize(size, op);

        if (in.readableBytes() < size) {
            return null;
        }
        if (size == 0) {
            return "";
        }

        byte[] argsBytes = new byte[size];
        in.readBytes(argsBytes);

        return new String(argsBytes);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.error("Codec error", cause);
        super.exceptionCaught(ctx, cause);
    }
}
