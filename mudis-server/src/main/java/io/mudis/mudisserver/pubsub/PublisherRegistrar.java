package io.mudis.mudisserver.pubsub;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Registry for managing publisher instances per channel.
 * Each publisher manages its own subscribers.
 */
public enum PublisherRegistrar {
    INSTANCE();

    private static final Logger Log = LoggerFactory.getLogger(PublisherRegistrar.class);

    private final ScheduledExecutorService executor;
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    PublisherRegistrar() {
        var threadFactory = Thread
                .ofPlatform()
                .daemon()
                .factory();

        this.executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
        this.executor.scheduleAtFixedRate(
                this::cleanupUnusedChannels,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    public Publisher getOrCreate(String channel) {
        validateChannel(channel);
        return publishers.computeIfAbsent(channel, _ -> {
            Publisher publisher = new Publisher();
            Log.info("Created publisher for channel: {}", channel);
            return publisher;
        });
    }

    public Publisher get(String channel) {
        return publishers.get(channel);
    }

    public boolean remove(String channel) {
        Publisher publisher = publishers.remove(channel);
        if (publisher != null) {
            publisher.close();
            Log.info("Removed publisher for channel: {}", channel);
            return true;
        }
        return false;
    }

    public void cleanupUnusedChannels() {
        int removed = 0;
        for (Map.Entry<String, Publisher> entry : publishers.entrySet()) {
            if (entry.getValue().getSubscriberCount() == 0) {
                if (remove(entry.getKey())) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            Log.debug("Cleaned up {} inactive channel(s)", removed);
        }
    }

    /**
     * Unsubscribe a context from all channels (used when client disconnects).
     */
    public void unsubscribeFromAll(ChannelHandlerContext ctx) {
        publishers.forEach((channel, publisher) -> {
            if (publisher.isSubscribed(ctx)) {
                publisher.unsubscribe(ctx);
                Log.debug("Unsubscribed context from channel: {}", channel);
            }
        });
    }

    public void shutdown() {
        Log.info("Shutting down PublishRegistrar with {} channel(s)", publishers.size());
        publishers.values().forEach(publisher -> {
            try {
                publisher.close();
            } catch (Exception e) {
                Log.error("Error closing publisher", e);
            }
        });
        publishers.clear();

        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        Log.info("Shutdown complete");
    }

    private void validateChannel(String channel) {
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty");
        }
    }
}
