package io.mudis.mudisclient.shell;

import io.mudis.mudisclient.client.Client;
import io.mudis.mudisclient.queue.MessageQueue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shell commands for pub/sub operations.
 */
@Component
@SuppressWarnings("unused")
public class PubSubCommands {
    private static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 5;
    private final MessageQueue messageQueue;
    private final Client client;

    @Autowired
    public PubSubCommands(Client client, MessageQueue messageQueue) {
        this.client = client;
        this.messageQueue = messageQueue;
    }

    @Command(name = "PUBLISH",
            description = "Publish a message to a channel",
            group = "Pub/Sub")
    public String publish(@Argument(index = 0, description = "Channel name") String channel,
                          @Argument(index = 1, description = "Message to publish") String message) {
        if (!client.isConnected()) {
            return "ERROR: Client is not connected. Run 'start' first.";
        }

        String cleanMessage = message.replace("\"", "");
        client.send("PUBLISH " + channel + " " + cleanMessage);

        return awaitServerResponse("Message sent");
    }

    @Command(name = "SUBSCRIBE",
            description = "Subscribe to a channel with optional data structure",
            group = "Pub/Sub")
    public String subscribe(
            @NotBlank @Argument(
                    index = 0,
                    description = "Channel name") String channel,
            @NotBlank @Argument(
                    index = 1,
                    description = "Data structure: [] (queue), #{} (set)",
                    defaultValue = "[]") String ds
    ) {
        if (!client.isConnected()) {
            return "ERROR: Client is not connected. Run 'start' first.";
        }

        ds = validateDataStructure(ds);
        if (ds == null) {
            return "ERROR: Invalid data structure. Use [] for queue, #{} for set.";
        }

        String command = "SUBSCRIBE " + channel + " " + ds;
        client.send(command);

        return awaitServerResponse("Subscription request sent");
    }

    @Command(name = "UNSUBSCRIBE",
            description = "Unsubscribe from a channel",
            group = "Pub/Sub")
    public String unsubscribe(
            @Argument(index = 0, description = "Channel name") String channel) {
        if (!client.isConnected()) {
            return "ERROR: Client is not connected. Run 'start' first.";
        }

        client.send("UNSUBSCRIBE " + channel);
        return awaitServerResponse("Unsubscribe request sent");
    }

    private String validateDataStructure(String ds) {
        return switch (ds.trim()) {
            case "[]" -> "[]";
            case "#{}" -> "#{}";
            default -> null;
        };
    }

    private String awaitServerResponse(String prefix) {
        var messages = new StringBuilder("\n");
        var future = new CompletableFuture<Void>();
        var counter = new AtomicInteger(1);

        Flow.Subscriber<String> subscriber = new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(String item) {
                if (counter.get() == 1) {
                    messages.append(item);
                } else {
                    messages.append(item).append("\n");
                }

                if (counter.incrementAndGet() > 1) {
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
            future.get(DEFAULT_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return prefix + messages;
        } catch (TimeoutException e) {
            return prefix + " (timeout waiting for server response)";
        } catch (Exception e) {
            return prefix + " (error: " + e.getMessage() + ")";
        }
    }
}
