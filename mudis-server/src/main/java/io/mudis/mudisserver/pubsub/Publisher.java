package io.mudis.mudisserver.pubsub;

import io.mudis.mudisserver.model.DataStructure;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Publisher that manages its own subscribers and their data structures.
 * Each publisher instance maintains its own set of subscriber contexts.
 */
public class Publisher extends SubmissionPublisher<String> {
    private static final Logger Log = LoggerFactory.getLogger(Publisher.class);
    private final Set<ChannelHandlerContext> subscribers = ConcurrentHashMap.newKeySet();
    private final Map<ChannelHandlerContext, DataStructureSubscriber> subscriberMap = new ConcurrentHashMap<>();

    public void subscribe(DataStructure ds, ChannelHandlerContext ctx) {
        var subscriber = new DataStructureSubscriber(ds, ctx);

        subscribers.add(ctx);
        subscriberMap.put(ctx, subscriber);
        super.subscribe(subscriber);

        Log.info("Client subscribed with data structure: {} (total: {})", ds, subscribers.size());
    }

    public void unsubscribe(ChannelHandlerContext ctx) {
        DataStructureSubscriber subscriber = subscriberMap.remove(ctx);

        if (subscriber != null) {
            subscribers.remove(ctx);
            if (subscriber.subscription != null) {
                subscriber.subscription.cancel();
            }
            Log.info("Client unsubscribed (remaining: {})", subscribers.size());
        } else {
            Log.warn("Attempted to unsubscribe non-existent subscriber");
        }
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }

    public boolean isSubscribed(ChannelHandlerContext ctx) {
        return subscribers.contains(ctx);
    }

    /**
     * Subscriber implementation that handles different data structure types.
     */
    private class DataStructureSubscriber implements Flow.Subscriber<String> {
        private final ChannelHandlerContext ctx;
        private final DataStructure dataStructure;
        private final Collection<String> collection;
        private Flow.Subscription subscription;

        DataStructureSubscriber(DataStructure dataStructure, ChannelHandlerContext ctx) {
            this.dataStructure = dataStructure;
            this.ctx = ctx;
            this.collection = createCollection(dataStructure);
        }

        private Collection<String> createCollection(DataStructure ds) {
            return switch (ds) {
                case LIST -> Collections.synchronizedList(new ArrayList<>());
                case HASH -> ConcurrentHashMap.newKeySet();
                case NONE -> null;
            };
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(1);
            this.subscription = subscription;
            Log.debug("Subscription established for {}", dataStructure);
        }

        @Override
        public void onNext(String message) {
            try {
                if (collection != null) {
                    collection.add(message);
                }
                subscription.request(1);
            } catch (Exception e) {
                Log.error("Error processing message: {}", message, e);
                onError(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            subscribers.remove(ctx);
            subscriberMap.remove(ctx);
            Log.error("Subscriber error", throwable);
        }

        @Override
        public void onComplete() {
            subscribers.remove(ctx);
            subscriberMap.remove(ctx);
            Log.info("Subscription completed");
        }
    }
}
