package io.mudis.mudis.command;

import io.mudis.mudis.client.Client;
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

    @Command(name = "PUBLISH",
            description = "Publish a message to a specific key in the Mudis Pub/Sub system.",
            group = "Pub/Sub")
    private String publish(
            @Option(required = true,
                    description = "The key to publish the message under.") String key,
            @Option(required = true,
                    description = "The message to publish.") String message) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        client.send("PUBLISH " + key + " " + message);
        return "Ok";
    }

    @Command(name = "SUBSCRIBE",
            description = "Subscribe to a key in the Mudis Pub/Sub system. Optionally specify the data structure.",
            group = "Pub/Sub")
    private String subscribe(
            @Option(required = true,
                    description = "The key to subscribe to.") String key,
            @Option(longName = "data-structure",
                    description = "Optional data structure type: [] for list, #{} for set. Leave empty for default.") String dataStructure) {
        if (!client.isConnected()) {
            return "Client is not connected. Please connect first.";
        }

        String ds = "";
        if (dataStructure != null && !dataStructure.isEmpty()) {
            switch (dataStructure) {
                case "[]":
                    ds = "[]";
                    break;
                case "#{}":
                    ds = "#{}";
                    break;
                default:
                    return "Invalid data-structure option. [ [] | #{} ]";
            }
        }

        client.send("SUBSCRIBE " + key + (ds.isEmpty() ? "" : " " + ds));
        return "Ok";
    }
}
