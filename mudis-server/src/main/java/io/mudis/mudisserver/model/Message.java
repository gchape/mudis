package io.mudis.mudisserver.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface Message {
    Pattern SUBSCRIBE_PATTERN = Pattern.compile("^([^ ]+)\\s*(.*)$");
    Pattern PUBLISH_PATTERN = Pattern.compile("^([^ ]+)\\s+(.*)$");
    Pattern UNSUBSCRIBE_PATTERN = Pattern.compile("^([^ ]+)$");

    static Message of(Operation op, String args) {
        return switch (op) {
            case SUBSCRIBE -> newSubscribeMessage(args);
            case PUBLISH -> newPublishMessage(args);
            case UNSUBSCRIBE -> newUnsubscribeMessage(args);
        };
    }

    private static Matcher getMatcher(Pattern pattern, String s) {
        Matcher matcher = pattern.matcher(s);
        if (!matcher.matches()) {
            throw new IllegalStateException("Invalid input: " + s);
        }
        return matcher;
    }

    private static Message newSubscribeMessage(String s) {
        Matcher matcher = getMatcher(SUBSCRIBE_PATTERN, s);
        String channel = matcher.group(1);
        String ds = matcher.group(2);

        return new Message.Subscribe(channel, DataStructure.from(ds));
    }

    private static Message newPublishMessage(String s) {
        Matcher matcher = getMatcher(PUBLISH_PATTERN, s);
        String channel = matcher.group(1);
        String message = matcher.group(2);

        return new Message.Publish(channel, message);
    }

    private static Message newUnsubscribeMessage(String s) {
        Matcher matcher = getMatcher(UNSUBSCRIBE_PATTERN, s);
        String channel = matcher.group(1);
        return new Message.Unsubscribe(channel);
    }

    record Subscribe(String channel, DataStructure ds) implements Message {
    }

    record Publish(String channel, String message) implements Message {
    }

    record Unsubscribe(String channel) implements Message {
    }
}
