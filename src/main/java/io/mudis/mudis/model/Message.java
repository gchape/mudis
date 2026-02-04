package io.mudis.mudis.model;

public sealed interface Message {

    static Message of(Op op, String[] args) {
        return switch (op) {
            case SUBSCRIBE -> newSubscribeMessage(args);
            case PUBLISH -> newPublishMessage(args);
        };
    }

    private static Message newSubscribeMessage(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalStateException("SUBSCRIBE requires 1-2 args, got " + args.length);
        }

        if (args.length == 1) {
            return new Message.Subscribe(args[0], Ds.NONE);
        } else {
            return new Message.Subscribe(args[0], Ds.from(args[1]));
        }
    }

    private static Message newPublishMessage(String[] args) {
        if (args.length != 2) {
            throw new IllegalStateException("PUBLISH requires 2 args, got " + args.length);
        }

        return new Message.Publish(args[0], args[1]);
    }

    record Subscribe(
            String channel,
            Ds ds
    ) implements Message {
    }

    record Publish(
            String channel,
            String message
    ) implements Message {
    }
}
