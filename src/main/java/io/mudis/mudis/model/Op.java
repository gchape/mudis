package io.mudis.mudis.model;

public enum Op {
    // PUB [channel] [message] [optional: data_structure]
    // data_structure: [] for list append, #{} for set
    PUBLISH(1024 * 1024),  // 1MB - needs space for message + data structure

    // SUB [channel]
    SUBSCRIBE(256);  // 256 bytes - just channel name

    private final long maxArgsSize;

    Op(long maxArgsSize) {
        this.maxArgsSize = maxArgsSize;
    }

    public long maxArgSize() {
        return maxArgsSize;
    }
}
