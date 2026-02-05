package io.mudis.mudisserver.server;

import io.mudis.mudisserver.model.Message;
import io.mudis.mudisserver.pubsub.Publisher;
import io.mudis.mudisserver.pubsub.PublisherRegistrar;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming messages from clients and routes them to appropriate pub/sub operations.
 */
public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger Log = LoggerFactory.getLogger(ServerHandler.class);
    private final PublisherRegistrar publisherRegistrar;

    public ServerHandler() {
        this.publisherRegistrar = io.mudis.mudisserver.pubsub.PublisherRegistrar.INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg == null) {
            sendError(ctx, "Invalid message");
            return;
        }

        Log.debug("Received message: {}", msg);

        try {
            switch (msg) {
                case Message.Show show -> handleShow(ctx, show);
                case Message.Subscribe sub -> handleSubscribe(ctx, sub);
                case Message.Publish pub -> handlePublish(ctx, pub);
                case Message.Unsubscribe unsub -> handleUnsubscribe(ctx, unsub);
            }
        } catch (Exception e) {
            Log.error("Error handling message: {}", msg, e);
            sendError(ctx, "Error processing message: " + e.getMessage());
        }
    }

    private void handleShow(ChannelHandlerContext ctx, Message.Show show) {
        String channel = show.channel();

        Publisher publisher = publisherRegistrar.get(channel);
        if (publisher == null) {
            ctx.writeAndFlush("WARN: No channel found: " + channel);
            Log.warn("Show to non-existent channel: {}", channel);
            return;
        }

        var subscriber = publisher.getSubscriber(ctx);
        if (subscriber == null) {
            ctx.writeAndFlush("WARN: You are not currently subscribed to this channel: " + channel);
            Log.warn("Subscriber not found or closed for ctx: {}", ctx);
            return;
        }

        ctx.writeAndFlush(subscriber.toString());
    }

    private void handleSubscribe(ChannelHandlerContext ctx, Message.Subscribe sub) {
        String channel = sub.channel();

        Publisher publisher = publisherRegistrar.getOrCreate(channel);
        publisher.subscribe(sub.ds(), ctx);

        ctx.writeAndFlush("OK: Subscribed to channel: " + channel);
        Log.info("Client subscribed to channel: {}", channel);
    }

    private void handlePublish(ChannelHandlerContext ctx, Message.Publish pub) {
        String channel = pub.channel();
        String message = pub.message();

        Publisher publisher = publisherRegistrar.get(channel);
        if (publisher == null) {
            ctx.writeAndFlush("WARN: No subscribers for channel: " + channel);
            Log.warn("Publish to channel with no subscribers: {}", channel);
            return;
        }

        int lag = publisher.submit(message);
        int subscriberCount = publisher.getSubscriberCount();

        ctx.writeAndFlush(String.format("OK: Published to %d subscriber(s)", subscriberCount));
        Log.debug("Published to channel: {} ({} subscribers, lag: {})", channel, subscriberCount, lag);
    }

    private void handleUnsubscribe(ChannelHandlerContext ctx, Message.Unsubscribe unsub) {
        String channel = unsub.channel();

        Publisher publisher = publisherRegistrar.get(channel);
        if (publisher == null) {
            sendError(ctx, "Channel not found: " + channel);
            return;
        }

        if (!publisher.isSubscribed(ctx)) {
            sendError(ctx, "Not subscribed to channel: " + channel);
            return;
        }

        publisher.unsubscribe(ctx);
        ctx.writeAndFlush("OK: Unsubscribed from channel: " + channel);
        Log.info("Client unsubscribed from channel: {}", channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Log.debug("Client disconnected, cleaning up subscriptions");
        publisherRegistrar.unsubscribeFromAll(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Exception in server handler", cause);
        sendError(ctx, "Internal server error");
        ctx.close();
    }

    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        ctx.writeAndFlush("ERROR: " + errorMessage);
    }
}
