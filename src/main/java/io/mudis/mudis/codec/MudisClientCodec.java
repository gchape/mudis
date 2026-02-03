package io.mudis.mudis.codec;

import io.mudis.mudis.model.Op;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MudisClientCodec extends ByteToMessageCodec<String> {

    @Override
    protected void encode(ChannelHandlerContext ctx, String command, ByteBuf out) {
        String[] parts = command.trim().split("\\s+", 3);
        if (parts.length < 1) {
            throw new IllegalArgumentException("Empty command");
        }

        String opName = parts[0].toUpperCase();
        Op op = Op.valueOf(opName);

        out.writeInt(op.ordinal());

        String args = parts.length > 1 ? command.substring(opName.length()).trim() : "";
        byte[] argsBytes = args.getBytes(StandardCharsets.UTF_8);
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
        if (size < 0 || size > 1024 * 1024) {
            in.resetReaderIndex();
            throw new IllegalStateException("Invalid response size: " + size);
        }

        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return;
        }

        byte[] responseBytes = new byte[size];
        in.readBytes(responseBytes);
        String response = new String(responseBytes, StandardCharsets.UTF_8);

        out.add(response);
    }
}
