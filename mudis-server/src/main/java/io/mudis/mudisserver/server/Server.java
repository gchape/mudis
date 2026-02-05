package io.mudis.mudisserver.server;

public interface Server {
    void stop();

    void start();

    boolean isRunning();
}
