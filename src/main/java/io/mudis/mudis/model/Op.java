package io.mudis.mudis.model;

public enum Op {
    // PUB [channel] [message] [optional: data_structure]
    // data_structure: [] | #{}
    PUBLISH(1024 * 1024),
    // SUB [channel]
    SUBSCRIBE(256);

    private final long MAX_ARG_SIZE;

    Op(long MAX_ARG_SIZE) {
        this.MAX_ARG_SIZE = MAX_ARG_SIZE;
    }

    public long MAX_ARG_SIZE() {
        return MAX_ARG_SIZE;
    }
}
