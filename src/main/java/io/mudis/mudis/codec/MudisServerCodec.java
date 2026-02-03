package io.mudis.mudis.codec;

import io.mudis.mudis.model.Message;
import io.mudis.mudis.model.Op;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class MudisServerCodec extends ByteToMessageCodec<Message> {
    static final int HEADER_SIZE = 8;

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        if (msg instanceof Message.Subscribe(String channel)) {
            String response = "SUBSCRIBED:" + channel;
            writeString(out, response);
        } else if (msg instanceof Message.Publish pub) {
            String response = "PUBLISHED:" + pub.channel();
            writeString(out, response);
        }
    }

    private void writeString(ByteBuf out, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!hasNeededMin(in)) {
            return;
        }

        in.markReaderIndex();

        Op op = readAndValidateOp(in);
        String[] args = readAndValidateArgs(in, op);
        if (args == null) {
            return;
        }

        var message = Message.of(op, args);
        out.add(message);
    }

    private boolean hasNeededMin(ByteBuf in) {
        return in.readableBytes() >= HEADER_SIZE;
    }

    private Op readAndValidateOp(ByteBuf in) {
        int opOrdinal = in.readInt();
        if (opOrdinal < 0 || opOrdinal >= Op.values().length) {
            in.resetReaderIndex();
            throw new IllegalStateException("Invalid op ordinal: " + opOrdinal);
        }
        return Op.values()[opOrdinal];
    }

    private String[] readAndValidateArgs(ByteBuf in, Op op) {
        int size = in.readInt();
        if (size < 0 || size > op.MAX_ARG_SIZE()) {
            in.resetReaderIndex();
            throw new IllegalStateException("Invalid args size: " + size);
        }

        if (in.readableBytes() < size) {
            in.resetReaderIndex();
            return null;
        }

        byte[] argsBytes = new byte[size];
        in.readBytes(argsBytes);

        return size > 0
                ? new String(argsBytes, StandardCharsets.UTF_8).split(" ")
                : new String[0];
    }
}
