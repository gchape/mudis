package io.mudis.mudis.server;

public interface Server {
    void start(String host, int port);

    boolean isRunning();

    void stop();
}
