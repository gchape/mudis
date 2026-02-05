package io.mudis.mudis.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public enum PublishRegistrar {
    INSTANCE;

    private static final Logger Log = LoggerFactory.getLogger(PublishRegistrar.class);
    private final Map<String, Publisher> publishers = new ConcurrentHashMap<>();

    public Publisher getOrCreate(String channel, Supplier<Publisher> supplier) {
        if (channel == null || channel.trim().isEmpty()) {
            throw new IllegalArgumentException("Channel name cannot be null or empty");
        }

        return publishers.computeIfAbsent(channel, _ -> {
            Publisher publisher = supplier.get();
            Log.info("Created publisher for channel: {}", channel);
            return publisher;
        });
    }

    public Publisher get(String channel) {
        return publishers.get(channel);
    }

    public Set<String> getChannels() {
        return Set.copyOf(publishers.keySet());
    }

    public boolean remove(String channel) {
        Publisher publisher = publishers.remove(channel);
        if (publisher != null) {
            publisher.close();
            Log.info("Removed and closed publisher for channel: {}", channel);
            return true;
        }
        return false;
    }

    public int cleanupInactiveChannels() {
        int removed = 0;
        for (Map.Entry<String, Publisher> entry : publishers.entrySet()) {
            if (entry.getValue().getSubscriberCount() == 0) {
                if (remove(entry.getKey())) {
                    removed++;
                }
            }
        }
        if (removed > 0) {
            Log.info("Cleaned up {} inactive channels", removed);
        }
        return removed;
    }

    public void shutdown() {
        Log.info("Shutting down PublishRegistrar with {} channels", publishers.size());
        publishers.forEach((channel, publisher) -> {
            try {
                publisher.close();
            } catch (Exception e) {
                Log.error("Error closing publisher for channel: {}", channel, e);
            }
        });
        publishers.clear();
        Log.info("PublishRegistrar shutdown complete");
    }
}
