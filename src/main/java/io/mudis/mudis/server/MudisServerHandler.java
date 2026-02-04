package io.mudis.mudis.server;

import io.mudis.mudis.model.Message;
import io.mudis.mudis.pubsub.PublishRegistrar;
import io.mudis.mudis.pubsub.Publisher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MudisServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MudisServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        LOGGER.info("Received message: {}", msg);

        if (msg instanceof Message.Subscribe sub) {
            handleSubscribe(ctx, sub);
        } else if (msg instanceof Message.Publish pub) {
            handlePublish(ctx, pub);
        }
    }

    private void handleSubscribe(ChannelHandlerContext ctx, Message.Subscribe sub) {
        LOGGER.info("Client subscribing to channel: {}", sub.channel());

        PublishRegistrar
                .INSTANCE
                .put(sub.channel(), Publisher::new);

        PublishRegistrar
                .INSTANCE
                .get(sub.channel())
                .subscribe(sub.ds(), ctx::writeAndFlush);
    }

    private void handlePublish(ChannelHandlerContext ctx, Message.Publish pub) {
        LOGGER.info("Client publishing to channel: {} with message: {}",
                pub.channel(), pub.message());

        Publisher publisher = PublishRegistrar.INSTANCE.get(pub.channel());
        if (publisher == null) {
            LOGGER.warn("No publisher for channel: {}", pub.channel());
            ctx.writeAndFlush("ERROR:Channel not found");
            return;
        }
        publisher.submit(pub.message());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Error in server handler", cause);
        ctx.close();
    }
}
