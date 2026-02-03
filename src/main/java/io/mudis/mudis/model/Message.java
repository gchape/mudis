package io.mudis.mudis.model;

public sealed interface Message {

    static Message of(Op op, String[] args) {
        return switch (op) {
            case SUBSCRIBE -> newSubscribeMessage(args);
            case PUBLISH -> newPublishMessage(args);
        };
    }

    private static Message newSubscribeMessage(String[] args) {
        if (args.length != 1) {
            throw new IllegalStateException("SUBSCRIBE requires 1 arg, got " + args.length);
        }
        return new Message.Subscribe(args[0]);
    }

    private static Message newPublishMessage(String[] args) {
        if (args.length < 2 || args.length > 3) {
            throw new IllegalStateException("PUBLISH requires 2-3 args, got " + args.length);
        }
        Ds ds = args.length == 3 ? Ds.from(args[2]) : Ds.NONE;
        return new Message.Publish(args[0], args[1], ds);
    }

    record Subscribe(String channel) implements Message {
    }

    record Publish(
            String channel,
            String message,
            Ds storage
    ) implements Message {
    }
}
