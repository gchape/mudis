package io.mudis.mudisclient.shell;

import io.mudis.mudisclient.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unused")
public class ClientCommands {
    private final Client client;

    @Autowired
    public ClientCommands(Client client) {
        this.client = client;
    }

    @Command(name = "start", description = "Start the Mudis client", group = "Client")
    public String startClient() {
        var result = new StringBuilder();

        if (!client.isConnected()) {
            try {
                client.connect();
                result.append("✓ Client connected");
            } catch (Exception e) {
                result.append("✗ Client connection failed: ").append(e.getMessage());
                return result.toString();
            }
        } else {
            result.append("- Client already connected\n");
        }

        return result.toString();
    }
}
