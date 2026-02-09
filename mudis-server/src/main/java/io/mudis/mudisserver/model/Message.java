package io.mudis.mudisserver.model;

import io.mudis.mudisshared.model.Operation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public sealed interface Message {
    // Matches a single channel name (e.g., SHOW <channel>)
    Pattern SHOW_CHANNEL_PATTERN = Pattern.compile("^([^ ]+)$");
    // Matches subscribe with channel and data structure (e.g., SUBSCRIBE <channel> <data_structure>)
    Pattern SUBSCRIBE_PATTERN = Pattern.compile("^([^ ]+)\\s+(.*)$");
    // Matches publish with channel and message (e.g., PUBLISH <channel> <message>)
    Pattern PUBLISH_PATTERN = Pattern.compile("^([^ ]+)\\s+(.*)$");
    // Matches unsubscribe with a single channel (e.g., UNSUBSCRIBE <channel>)
    Pattern UNSUBSCRIBE_PATTERN = Pattern.compile("^([^ ]+)$");

    static Message of(Operation op, String args) {
        return switch (op) {
            case SHOW -> newShowMessage(args);
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

    private static Message newShowMessage(String s) {
        Matcher matcher = getMatcher(SHOW_CHANNEL_PATTERN, s);
        String channel = matcher.group(1);
        return new Show(channel);
    }

    private static Message newSubscribeMessage(String s) {
        Matcher matcher = getMatcher(SUBSCRIBE_PATTERN, s);
        String channel = matcher.group(1);
        String ds = matcher.group(2);
        return new Subscribe(channel, DataStructure.from(ds));
    }

    private static Message newPublishMessage(String s) {
        Matcher matcher = getMatcher(PUBLISH_PATTERN, s);
        String channel = matcher.group(1);
        String message = matcher.group(2);
        return new Publish(channel, message);
    }

    private static Message newUnsubscribeMessage(String s) {
        Matcher matcher = getMatcher(UNSUBSCRIBE_PATTERN, s);
        String channel = matcher.group(1);
        return new Unsubscribe(channel);
    }

    record Show(String channel) implements Message {
    }

    record Subscribe(String channel, DataStructure ds) implements Message {
    }

    record Publish(String channel, String message) implements Message {
    }

    record Unsubscribe(String channel) implements Message {
    }
}
