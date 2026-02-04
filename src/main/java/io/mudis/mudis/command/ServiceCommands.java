package io.mudis.mudis.command;

import io.mudis.mudis.client.Client;
import io.mudis.mudis.server.MudisServer;
import io.mudis.mudis.server.Server;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
public class ServiceCommands implements DisposableBean {
    private final Client client;
    private final Server server;

    @Autowired
    public ServiceCommands(MudisServer mudisServer, Client client) {
        this.client = client;
        this.server = mudisServer;
    }

    @Command(name = "start",
            description = "Start the Mudis service.",
            group = "Service")
    public String startService() {
        String status = "";
        if (!server.isRunning()) {
            Thread.startVirtualThread(server::start);
            status = "Mudis service has started successfully.";
        }

        if (!client.isConnected()) {
            Thread.startVirtualThread(client::connect);
            status = "Mudis service has started successfully.";
        }

        if (status.isEmpty()) {
            status = "Mudis service is already running.";
        }

        return status;
    }

    @Override
    public void destroy() {
        if (server.isRunning()) {
            server.stop();
        }

        if (client.isConnected()) {
            client.disconnect();
        }
    }
}
