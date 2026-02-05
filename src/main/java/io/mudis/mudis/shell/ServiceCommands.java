package io.mudis.mudis.shell;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.pubsub.PublishRegistrar;
import io.mudis.mudis.server.ServerImpl;
import io.mudis.mudis.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import java.net.Socket;
import java.util.concurrent.CompletableFuture;

@Component
public class ServiceCommands {
    private final Client client;
    private final Server server;
    private CompletableFuture<Void> serverFuture;

    @Value("${mudis.server.host}")
    private String host;
    @Value("${mudis.server.port}")
    private int port;

    @Autowired
    public ServiceCommands(ServerImpl mudisServer, Client client) {
        this.client = client;
        this.server = mudisServer;
    }

    public boolean isServerRunning() {
        try (Socket socket = new Socket(this.host, this.port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Command(name = "start", description = "Start the Mudis service.", group = "Service")
    public String startService() {
        StringBuilder status = new StringBuilder();

        if (!isServerRunning()) {
            serverFuture = CompletableFuture.runAsync(server::start);
            status.append("✓ Server started\n");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            status.append("- Server already running\n");
        }

        if (!client.isConnected()) {
            try {
                client.connect();
                status.append("✓ Client connected\n");
            } catch (Exception e) {
                status.append("✗ Client connection failed: ").append(e.getMessage()).append("\n");
            }
        } else {
            status.append("- Client already connected\n");
        }

        return (!status.isEmpty() ? status + "Mudis service ready!" : "Mudis service is already running.") + System.lineSeparator();
    }

    @Command(name = "stop", description = "Stop the Mudis service.", group = "Service")
    public String stopService() {
        var status = new StringBuilder();
        if (client.isConnected()) {
            client.disconnect();
            status.append("✓ Client disconnected\n");
        }

        if (server.isRunning()) {
            server.stop();
            status.append("✓ Server stopped\n");

            if (serverFuture != null) {
                serverFuture.cancel(true);
            }
        }

        PublishRegistrar.INSTANCE.shutdown();
        status.append("✓ Pub/Sub system cleaned up\n");

        return !status.isEmpty() ? status + "Mudis service stopped!" : "Mudis service is not running.";
    }

    @Command(name = "status", description = "Show the current status of the Mudis service.", group = "Service")
    public String getStatus() {
        return "=== Mudis Status ===\n" +
                "Server: " + (server.isRunning() ? "RUNNING" : "STOPPED") + "\n" +
                "Client: " + (client.isConnected() ? "CONNECTED" : "DISCONNECTED") + "\n" +
                "Channels: " + PublishRegistrar.INSTANCE.getChannels().size() + "\n";
    }

    @Command(name = "channels", description = "List all active pub/sub channels.", group = "Service")
    public String listChannels() {
        var channels = PublishRegistrar.INSTANCE.getChannels();
        if (channels.isEmpty()) {
            return "No active channels.";
        }

        var sb = new StringBuilder();
        sb.append("=== Active Channels (").append(channels.size()).append(") ===\n");
        channels.forEach(channel -> sb.append("• ").append(channel).append("\n"));
        return sb.toString();
    }

    @Command(name = "cleanup", description = "Remove inactive channels with no subscribers.", group = "Service")
    public String cleanupChannels() {
        int removed = PublishRegistrar.INSTANCE.cleanupInactiveChannels();
        return "Cleaned up " + removed + " inactive channel(s).";
    }
}
