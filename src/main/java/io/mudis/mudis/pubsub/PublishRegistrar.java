package io.mudis.mudis.pubsub;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public enum PublishRegistrar {
    INSTANCE;

    final Map<String, Publisher> publishers;

    PublishRegistrar() {
        this.publishers = new ConcurrentHashMap<>();
    }

    public void put(String key, Supplier<Publisher> supplier) {
        publishers.putIfAbsent(key, supplier.get());
    }

    public Publisher get(String key) {
        return publishers.get(key);
    }
}
