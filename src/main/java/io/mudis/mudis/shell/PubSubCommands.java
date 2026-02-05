package io.mudis.mudis.shell;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.client.ClientHandler;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Argument;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
public class PubSubCommands {
    private final Client client;

    @Autowired
    public PubSubCommands(Client client) {
        this.client = client;
    }

    private @NonNull String waitForChannelResponseAndReturn(int n, String response) {
        var channelOut = new StringBuilder(System.lineSeparator());
        try {
            for (int i = 0; i < n; i++) {
                channelOut.append(ClientHandler.systemOutQueue.take())
                        .append(System.lineSeparator());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response + channelOut;
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

        if (message.charAt(0) != '\"' && message.charAt(message.length() - 1) != '\"') {
            return "Message should be quoted: \"message\".";
        }

        client.send("PUBLISH " + channel + " " + message.replace("\"", ""));
        return waitForChannelResponseAndReturn(2, "Message sent");
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
        return waitForChannelResponseAndReturn(1, "Subscription request sent");
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
        return waitForChannelResponseAndReturn(1, "Unsubscribe request sent");
    }
}
