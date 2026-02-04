package io.mudis.mudis.model;

public sealed interface Message {

    static Message of(Operation op, String[] args) {
        return switch (op) {
            case SUBSCRIBE -> newSubscribeMessage(args);
            case PUBLISH -> newPublishMessage(args);
            case UNSUBSCRIBE -> newUnsubscribeMessage(args);
        };
    }

    private static Message newSubscribeMessage(String[] args) {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalStateException("SUBSCRIBE requires 1-2 args, got " + args.length);
        }

        if (args.length == 1) {
            return new Message.Subscribe(args[0], DataStructure.NONE);
        } else {
            return new Message.Subscribe(args[0], DataStructure.from(args[1]));
        }
    }

    private static Message newPublishMessage(String[] args) {
        if (args.length != 2) {
            throw new IllegalStateException("PUBLISH requires 2 args, got " + args.length);
        }

        return new Message.Publish(args[0], args[1]);
    }

    private static Message newUnsubscribeMessage(String[] args) {
        if (args.length != 1) {
            throw new IllegalStateException("UNSUBSCRIBE requires 1 arg, got " + args.length);
        }

        return new Message.Unsubscribe(args[0]);
    }

    record Subscribe(
            String channel,
            DataStructure ds
    ) implements Message {
    }

    record Publish(
            String channel,
            String message
    ) implements Message {
    }

    record Unsubscribe(
            String channel
    ) implements Message {
    }
}
