package io.mudis.mudis.codec;

import io.mudis.mudis.model.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MudisClientCodec extends ByteToMessageCodec<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MudisClientCodec.class);
    private static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    protected void encode(ChannelHandlerContext ctx, String command, ByteBuf out) {
        String[] parts = command.trim().split("\\s+", 3);
        if (parts.length < 1) {
            throw new IllegalArgumentException("Empty command");
        }

        String opName = parts[0].toUpperCase();

        Operation op;
        try {
            op = Operation.valueOf(opName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown operation: " + opName + ". Valid operations: " + getValidOps()
            );
        }

        out.writeInt(op.ordinal());

        String args = parts.length > 1 ? command.substring(opName.length()).trim() : "";
        byte[] argsBytes = args.getBytes(StandardCharsets.UTF_8);

        if (argsBytes.length > op.MAX_ARG_SIZE()) {
            throw new IllegalArgumentException(
                    String.format("Arguments too long: %d bytes (max: %d)",
                            argsBytes.length, op.MAX_ARG_SIZE())
            );
        }

        out.writeInt(argsBytes.length);
        out.writeBytes(argsBytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();

        int size = in.readInt();
        if (size < 0 || size > MAX_RESPONSE_SIZE) {
            in.resetReaderIndex();
            throw new IllegalStateException(
                    String.format("Invalid response size: %d (max: %d)", size, MAX_RESPONSE_SIZE)
            );
        }

        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }

        byte[] response = new byte[size];
        in.readBytes(response);
        out.add(new String(response, StandardCharsets.UTF_8));
    }

    private String getValidOps() {
        Operation[] ops = Operation.values();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ops.length; i++) {
            sb.append(ops[i].name());
            if (i < ops.length - 1) sb.append(", ");
        }
        return sb.toString();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Codec error", cause);
        super.exceptionCaught(ctx, cause);
    }
}
