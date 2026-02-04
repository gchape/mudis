package io.mudis.mudis.pubsub;

import io.mudis.mudis.model.Ds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

public class Publisher extends SubmissionPublisher<String> {
    static final Logger LOGGER = LoggerFactory.getLogger(Publisher.class);

    public void subscribe(Ds ds, Consumer<String> consumer) {
        super.subscribe(new Flow.Subscriber<>() {
            private Collection<String> collection;
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                switch (ds) {
                    case LIST:
                        collection = Collections.synchronizedList(new ArrayList<>());
                        break;
                    case HASH:
                        collection = ConcurrentHashMap.newKeySet();
                        break;
                    default:
                        collection = null;
                }

                this.subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                if (collection != null) {
                    collection.add(item);
                    consumer.accept(collection.toString());
                    LOGGER.info("Item added to collection: {}", item);
                } else {
                    consumer.accept(item);
                    LOGGER.info("Received item: {}", item);
                }

                this.subscription.request(1);
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.error("Error occurred: ", throwable);
            }

            @Override
            public void onComplete() {
                LOGGER.info("Subscription completed.");
            }
        });
    }
}
