package io.mudis.mudis.shell;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.client.MudisClientHandler;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
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
                channelOut.append(MudisClientHandler.systemOutQueue.take())
                        .append(System.lineSeparator());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response + channelOut;
    }

    @Command(name = "PUBLISH",
            description = "Publish a message to a specific key in the Mudis Pub/Sub system.",
            group = "Pub/Sub")
    public String publish(
            @Option(required = true,
                    description = "The key to publish the message under.") String key,
            @Option(required = true,
                    description = "The message to publish.") String message) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        client.send("PUBLISH " + key + " " + message);
        return waitForChannelResponseAndReturn(2, "Message sent");
    }

    @Command(name = "SUBSCRIBE",
            description = "Subscribe to a key in the Mudis Pub/Sub system. Optionally specify the data structure.",
            group = "Pub/Sub")
    public String subscribe(
            @Option(required = true,
                    description = "The key to subscribe to.") String key,
            @Option(longName = "data-structure",
                    description = "Optional data structure type: [] for list, #{} for set. Leave empty for default.") String dataStructure) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        String ds = "";
        if (dataStructure != null && !dataStructure.isEmpty()) {
            ds = switch (dataStructure) {
                case "[]" -> "[]";
                case "#{}" -> "#{}";
                default -> null;
            };

            if (ds == null) {
                return "Invalid data-structure option. Valid options: [] or #{}";
            }
        }

        client.send("SUBSCRIBE " + key + (ds.isEmpty() ? "" : " " + ds));
        return waitForChannelResponseAndReturn(1, "Subscription request sent");
    }

    @Command(name = "UNSUBSCRIBE",
            description = "Unsubscribe from a channel in the Mudis Pub/Sub system.",
            group = "Pub/Sub")
    public String unsubscribe(
            @Option(required = true,
                    description = "The channel to unsubscribe from.") String channel) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        client.send("UNSUBSCRIBE " + channel);
        return waitForChannelResponseAndReturn(1, "Unsubscribe request sent");
    }
}
