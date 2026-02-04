package io.mudis.mudis.pubsub;

import io.mudis.mudis.model.DataStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

public class Publisher extends SubmissionPublisher<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    private final Map<Consumer<String>, Flow.Subscriber<String>> subscribers = new ConcurrentHashMap<>();

    public void subscribe(DataStructure ds, Consumer<String> consumer) {
        DataStructureSubscriber subscriber = new DataStructureSubscriber(ds, consumer);
        subscribers.put(consumer, subscriber);
        super.subscribe(subscriber);
        LOGGER.info("New subscriber added with data structure: {}", ds);
    }

    public void unsubscribe(Consumer<String> consumer) {
        Flow.Subscriber<String> subscriber = subscribers.remove(consumer);
        if (subscriber != null) {
            LOGGER.info("Subscriber removed");
            return;
        }
        LOGGER.warn("Subscriber not found");
    }

    public int getSubscriberCount() {
        return subscribers.size();
    }

    private class DataStructureSubscriber implements Flow.Subscriber<String> {
        private final DataStructure ds;
        private final Consumer<String> consumer;
        private Collection<String> collection;
        private Flow.Subscription subscription;

        public DataStructureSubscriber(DataStructure ds, Consumer<String> consumer) {
            this.ds = ds;
            this.consumer = consumer;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            this.collection = switch (ds) {
                case LIST -> Collections.synchronizedList(new ArrayList<>());
                case HASH -> ConcurrentHashMap.newKeySet();
                case NONE -> null;
            };
            this.subscription.request(1);
        }

        @Override
        public void onNext(String item) {
            try {
                if (collection != null) {
                    collection.add(item);
                    consumer.accept(collection.toString());
                    LOGGER.debug("Item added to {} collection: {}", ds, item);
                } else {
                    consumer.accept(item);
                    LOGGER.debug("Item forwarded: {}", item);
                }
                this.subscription.request(1);
            } catch (Exception e) {
                LOGGER.error("Error processing item: {}", item, e);
                onError(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            LOGGER.error("Subscriber error", throwable);
            subscribers.remove(consumer);
        }

        @Override
        public void onComplete() {
            LOGGER.info("Subscription completed");
            subscribers.remove(consumer);
        }
    }
}
