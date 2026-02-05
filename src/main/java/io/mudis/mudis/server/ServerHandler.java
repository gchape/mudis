package io.mudis.mudis.server;

import io.mudis.mudis.model.Message;
import io.mudis.mudis.pubsub.PublishRegistrar;
import io.mudis.mudis.pubsub.Publisher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ServerHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger Log = LoggerFactory.getLogger(ServerHandler.class);
    private final Map<String, Consumer<String>> channelSubscriptions = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        if (msg == null) {
            Log.warn("Received null message");
            sendError(ctx, "Invalid message");
            return;
        }

        Log.info("Received message: {}", msg);

        try {
            switch (msg) {
                case Message.Subscribe sub -> handleSubscribe(ctx, sub);
                case Message.Publish pub -> handlePublish(ctx, pub);
                case Message.Unsubscribe unsub -> handleUnsubscribe(ctx, unsub);
            }
        } catch (Exception e) {
            Log.error("Error handling message: {}", msg, e);
            sendError(ctx, "Error processing message: " + e.getMessage());
        }
    }

    private void handleSubscribe(ChannelHandlerContext ctx, Message.Subscribe sub) {
        String channel = sub.channel();

        if (channel == null || channel.trim().isEmpty()) {
            sendError(ctx, "Channel name cannot be empty");
            return;
        }

        Log.info("Client subscribing to channel: {}", channel);

        Publisher publisher = PublishRegistrar.INSTANCE.getOrCreate(
                channel,
                Publisher::new
        );

        Consumer<String> consumer = ctx::writeAndFlush;
        channelSubscriptions.put(channel, consumer);

        publisher.subscribe(sub.ds(), consumer);
        ctx.writeAndFlush("OK: Subscribed to channel: " + channel);

        Log.info("Client successfully subscribed to channel: {}", channel);
    }

    private void handlePublish(ChannelHandlerContext ctx, Message.Publish pub) {
        var channel = pub.channel();
        var message = pub.message();

        if (channel == null || channel.trim().isEmpty()) {
            sendError(ctx, "Channel name cannot be empty");
            return;
        }

        if (message == null) {
            sendError(ctx, "Message cannot be null");
            return;
        }

        Log.info("Client publishing to channel: {}", channel);

        Publisher publisher = PublishRegistrar.INSTANCE.get(channel);
        if (publisher == null) {
            ctx.writeAndFlush("WARN: No subscribers for channel: " + channel);
            return;
        }

        int lag = publisher.submit(message);
        ctx.writeAndFlush(String.format("OK: Published to %d subscriber(s)",
                publisher.getSubscriberCount()));

        Log.debug("Message published to channel: {} (lag: {})", channel, lag);
    }

    private void handleUnsubscribe(ChannelHandlerContext ctx, Message.Unsubscribe unsub) {
        String channel = unsub.channel();

        if (channel == null || channel.trim().isEmpty()) {
            sendError(ctx, "Channel name cannot be empty");
            return;
        }

        Log.info("Client unsubscribing from channel: {}", channel);

        Consumer<String> consumer = channelSubscriptions.remove(channel);
        if (consumer == null) {
            sendError(ctx, "Not subscribed to channel: " + channel);
            return;
        }

        Publisher publisher = PublishRegistrar.INSTANCE.get(channel);
        if (publisher != null) {
            publisher.unsubscribe(consumer);
            ctx.writeAndFlush("OK: Unsubscribed from channel: " + channel);
            Log.info("Client unsubscribed from channel: {}", channel);
        } else {
            sendError(ctx, "Channel not found: " + channel);
        }
    }

    private void sendError(ChannelHandlerContext ctx, String errorMessage) {
        ctx.writeAndFlush("ERROR: " + errorMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channelSubscriptions.forEach((channel, consumer) -> {
            Publisher publisher = PublishRegistrar.INSTANCE.get(channel);
            if (publisher != null) {
                publisher.unsubscribe(consumer);
            }
        });
        channelSubscriptions.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.error("Exception in server handler", cause);
        sendError(ctx, "Internal server error");
        ctx.close();
    }
}
