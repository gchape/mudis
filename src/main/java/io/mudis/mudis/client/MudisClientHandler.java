package io.mudis.mudis.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MudisClientHandler extends SimpleChannelInboundHandler<String> {
    public static final BlockingQueue<String> systemOutQueue;
    private static final Logger Log;

    static {
        systemOutQueue = new LinkedBlockingQueue<>();
        Log = LoggerFactory.getLogger(MudisClientHandler.class);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String response) {
        systemOutQueue.add(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Client error", cause);
        ctx.close();
    }
}
