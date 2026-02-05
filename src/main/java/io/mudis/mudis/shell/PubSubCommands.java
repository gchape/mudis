package io.mudis.mudis.shell;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.mq.MessageQueue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PubSubCommands {
    private final Client client;
    private final MessageQueue messageQueue;

    @Autowired
    public PubSubCommands(Client client, MessageQueue messageQueue) {
        this.client = client;
        this.messageQueue = messageQueue;
    }

    private @NonNull String awaitServerMessages(int count, String prefix) {
        var messages = new StringBuilder(System.lineSeparator());

        var future = new CompletableFuture<Void>();
        var counter = new AtomicInteger(0);
        var subscriber = new Flow.Subscriber<String>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(count);
            }

            @Override
            public void onNext(String item) {
                messages.append(item).append(System.lineSeparator());
                counter.incrementAndGet();

                if (counter.get() >= count) {
                    subscription.cancel();
                    future.complete(null);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(null);
            }
        };

        messageQueue.subscribe(subscriber);
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            return prefix + " (timeout waiting for server response)";
        }

        return prefix + messages;
    }

    @Command(name = "PUBLISH",
            description = "Publish a message to a specific channel in the Mudis Pub/Sub system.",
            group = "Pub/Sub")
    public String publish(
            @NotBlank
            @Argument(index = 0,
                    description = "The channel to publish the message under.")
            String channel,

            @NotBlank
            @Argument(index = 1,
                    description = "The message to publish.")
            String message) {

        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        client.send("PUBLISH " + channel + " " + message.replace("\"", ""));
        return awaitServerMessages(2, "Message sent");
    }

    @Command(name = "SUBSCRIBE",
            description = "Subscribe to a channel in the Mudis Pub/Sub system. Optionally specify the data structure.",
            group = "Pub/Sub")
    public String subscribe(
            @NotBlank
            @Argument(index = 0,
                    description = "The channel to subscribe to.")
            String channel,

            @Argument(index = 1,
                    description = "Optional data structure type: [] for list, #{} for set. Leave empty for default.")
            String ds) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        if (!ds.isBlank()) {
            ds = switch (ds) {
                case "[]" -> "[]";
                case "#{}" -> "#{}";
                default -> null;
            };

            if (ds == null) {
                return "Invalid data-structure option. Valid options: [] or #{}";
            }
        }

        client.send("SUBSCRIBE " + channel + (ds.isBlank() ? "" : " " + ds));
        return awaitServerMessages(1, "Subscription request sent");
    }

    @Command(name = "UNSUBSCRIBE",
            description = "Unsubscribe from a channel in the Mudis Pub/Sub system.",
            group = "Pub/Sub")
    public String unsubscribe(
            @NotNull
            @Argument(index = 0,
                    description = "The channel to unsubscribe from.")
            String channel) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        client.send("UNSUBSCRIBE " + channel);
        return awaitServerMessages(1, "Unsubscribe request sent");
    }
}
