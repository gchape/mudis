package io.mudis.mudis.shell;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.pubsub.PublisherRegistrar;
import io.mudis.mudis.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.concurrent.CompletableFuture;

/**
 * Shell commands for managing the Mudis service lifecycle.
 */
@Component
public class ServiceCommands {
    private final Client client;
    private final Server server;
    private final PublisherRegistrar publishRegistrar;
    private CompletableFuture<Void> serverFuture;

    @Value("${mudis.server.host:localhost}")
    private String host;

    @Value("${mudis.server.port:6379}")
    private int port;

    public ServiceCommands(Server server, Client client, PublisherRegistrar publishRegistrar) {
        this.client = client;
        this.server = server;
        this.publishRegistrar = publishRegistrar;
    }

    @Command(name = "start", description = "Start the Mudis service", group = "Service")
    public String startService() {
        StringBuilder result = new StringBuilder();

        // Start server if not running
        if (!isServerReachable()) {
            serverFuture = CompletableFuture.runAsync(server::start);
            result.append("✓ Server started\n");
            waitForServerStartup();
        } else {
            result.append("- Server already running\n");
        }

        // Connect client if not connected
        if (!client.isConnected()) {
            try {
                client.connect();
                result.append("✓ Client connected\n");
            } catch (Exception e) {
                result.append("✗ Client connection failed: ").append(e.getMessage()).append("\n");
                return result.toString();
            }
        } else {
            result.append("- Client already connected\n");
        }

        result.append("Mudis service ready!\n");
        return result.toString();
    }

    @Command(name = "stop", description = "Stop the Mudis service", group = "Service")
    public String stopService() {
        StringBuilder result = new StringBuilder();

        if (client.isConnected()) {
            client.disconnect();
            result.append("✓ Client disconnected\n");
        }

        if (server.isRunning()) {
            server.stop();
            result.append("✓ Server stopped\n");

            if (serverFuture != null) {
                serverFuture.cancel(true);
                serverFuture = null;
            }
        }

        publishRegistrar.shutdown();
        result.append("✓ Pub/Sub system cleaned up\n");

        result.append("Mudis service stopped!\n");
        return result.toString();
    }

    @Command(name = "status", description = "Show the current status of the Mudis service", group = "Service")
    public String getStatus() {
        return String.format("""
                        === Mudis Status ===
                        Server: %s
                        Client: %s
                        Active Channels: %d
                        """,
                server.isRunning() ? "RUNNING" : "STOPPED",
                client.isConnected() ? "CONNECTED" : "DISCONNECTED",
                publishRegistrar.getChannels().size()
        );
    }

    @Command(name = "channels", description = "List all active pub/sub channels", group = "Service")
    public String listChannels() {
        var channels = publishRegistrar.getChannels();

        if (channels.isEmpty()) {
            return "No active channels.";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== Active Channels (").append(channels.size()).append(") ===\n");
        channels.forEach(channel -> result.append("• ").append(channel).append("\n"));

        return result.toString();
    }

    @Command(name = "cleanup", description = "Remove inactive channels with no subscribers", group = "Service")
    public String cleanupChannels() {
        int removed = publishRegistrar.cleanupInactiveChannels();
        return "Cleaned up " + removed + " inactive channel(s).";
    }

    private boolean isServerReachable() {
        try (Socket socket = new Socket(host, port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForServerStartup() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
