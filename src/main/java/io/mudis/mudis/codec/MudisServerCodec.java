package io.mudis.mudis.codec;

import io.mudis.mudis.model.Message;
import io.mudis.mudis.model.Command;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MudisServerCodec extends ByteToMessageCodec<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MudisServerCodec.class);

    private static final int HEADER_SIZE = 8;
    private static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        try {
            Command op = readOperation(in);
            String[] args = readArguments(in, op);

            if (args == null) {
                in.resetReaderIndex();
                return;
            }

            Message message = Message.of(op, args);
            out.add(message);

        } catch (IllegalStateException | IllegalArgumentException e) {
            LOGGER.error("Error decoding message", e);
            in.resetReaderIndex();
            throw e;
        }
    }

    private Command readOperation(ByteBuf in) {
        int opOrdinal = in.readInt();
        if (opOrdinal < 0 || opOrdinal >= Command.values().length) {
            throw new IllegalStateException(
                    String.format("Invalid operation ordinal: %d (valid: 0-%d)",
                            opOrdinal, Command.values().length - 1)
            );
        }
        return Command.values()[opOrdinal];
    }

    private String[] readArguments(ByteBuf in, Command op) {
        int size = in.readInt();

        if (size < 0) {
            throw new IllegalStateException("Negative arguments size: " + size);
        }

        if (size > op.MAX_ARG_SIZE()) {
            throw new IllegalStateException(
                    String.format("Arguments size %d exceeds maximum %d for %s",
                            size, op.MAX_ARG_SIZE(), op)
            );
        }

        if (size > MAX_MESSAGE_SIZE) {
            throw new IllegalStateException(
                    String.format("Arguments size %d exceeds global maximum %d",
                            size, MAX_MESSAGE_SIZE)
            );
        }

        if (in.readableBytes() < size) {
            return null; // Not enough bytes yet
        }

        if (size == 0) {
            return new String[0];
        }

        byte[] argsBytes = new byte[size];
        in.readBytes(argsBytes);

        String argsString = new String(argsBytes, StandardCharsets.UTF_8);
        return argsString.split(" ");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Codec error", cause);
        super.exceptionCaught(ctx, cause);
    }
}
