package io.mudis.mudis.client;

import io.mudis.mudis.mq.MessageQueue;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger Log = LoggerFactory.getLogger(ClientHandler.class);
    private final MessageQueue messageQueue;

    public ClientHandler(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String response) {
        messageQueue.submit(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Client error", cause);
        ctx.close();
    }
}
