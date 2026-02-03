package io.mudis.mudis.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MudisClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MudisClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String response) {
        // TODO: Add response logic
        LOGGER.info("Received from server: {}", response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Client error", cause);
        ctx.close();
    }
}
