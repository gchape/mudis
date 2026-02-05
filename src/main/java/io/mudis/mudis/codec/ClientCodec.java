package io.mudis.mudis.codec;

import io.mudis.mudis.model.Operation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static io.mudis.mudis.utils.RequestValidator.validateArgsBytes;

public class ClientCodec extends ByteToMessageCodec<String> {
    private static final Logger Log = LoggerFactory.getLogger(ClientCodec.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, String command, ByteBuf out) {
        String[] parts = command.trim().split("\\s+");

        if (parts.length == 0) {
            throw new IllegalArgumentException("Empty command");
        }

        String opName = parts[0];
        Operation op;
        try {
            op = Operation.valueOf(opName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown operation: " + opName + ". Valid operations: " + Operation.asString(), e);
        }

        out.writeInt(op.ordinal());

        var args = String.join(" ",
                Arrays.copyOfRange(parts, 1, parts.length));
        byte[] argsBytes = (parts.length > 1)
                ? args.getBytes()
                : new byte[0];

        validateArgsBytes(argsBytes, op);

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
        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }

        byte[] response = new byte[size];
        in.readBytes(response);
        out.add(new String(response, StandardCharsets.UTF_8));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Log.error("Codec error", cause);
        super.exceptionCaught(ctx, cause);
    }
}
