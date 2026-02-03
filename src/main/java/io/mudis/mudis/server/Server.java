package io.mudis.mudis.server;

public interface Server {
    void stop();

    void start();

    boolean isRunning();
}
