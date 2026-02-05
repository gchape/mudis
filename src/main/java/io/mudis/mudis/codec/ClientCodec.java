package io.mudis.mudis.codec;

import io.mudis.mudis.model.Operation;
import io.mudis.mudis.utils.RequestValidator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Codec for encoding client commands and decoding server responses.
 * Protocol: [operation_ordinal:int][args_length:int][args:bytes]
 */
public class ClientCodec extends ByteToMessageCodec<String> {
    private static final Logger Log = LoggerFactory.getLogger(ClientCodec.class);
    private static final int MIN_HEADER_SIZE = 4; // For response length

    @Override
    protected void encode(ChannelHandlerContext ctx, String command, ByteBuf out) {
        String[] parts = command.trim().split("\\s+", 2);

        if (parts.length == 0 || parts[0].isEmpty()) {
            throw new IllegalArgumentException("Empty command");
        }

        Operation op = parseOperation(parts[0]);
        byte[] argsBytes = getArgumentsBytes(parts);

        RequestValidator.validateArgsBytes(argsBytes, op);

        out.writeInt(op.ordinal());
        out.writeInt(argsBytes.length);
        out.writeBytes(argsBytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < MIN_HEADER_SIZE) {
            return;
        }

        in.markReaderIndex();

        int size = in.readInt();
        if (size < 0) {
            throw new IllegalStateException("Negative response size: " + size);
        }

        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }

        byte[] response = new byte[size];
        in.readBytes(response);
        out.add(new String(response, StandardCharsets.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Codec error", cause);
        ctx.close();
    }

    private Operation parseOperation(String opName) {
        try {
            return Operation.valueOf(opName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown operation: " + opName + ". Valid operations: " + Operation.asString(), e
            );
        }
    }

    private byte[] getArgumentsBytes(String[] parts) {
        if (parts.length < 2 || parts[1].isEmpty()) {
            return new byte[0];
        }
        return parts[1].getBytes(StandardCharsets.UTF_8);
    }
}
