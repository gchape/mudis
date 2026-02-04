package io.mudis.mudis.model;

public enum Operation {
    // PUBLISH [channel] [message]
    PUBLISH(1024 * 1024),
    // SUBSCRIBE [channel] [optional: data_structure]
    // data_structure: [] | #{}
    SUBSCRIBE(256),
    // UNSUBSCRIBE [channel]
    UNSUBSCRIBE(256);

    private final long MAX_ARG_SIZE;

    Operation(long MAX_ARG_SIZE) {
        this.MAX_ARG_SIZE = MAX_ARG_SIZE;
    }

    public long MAX_ARG_SIZE() {
        return MAX_ARG_SIZE;
    }
}
