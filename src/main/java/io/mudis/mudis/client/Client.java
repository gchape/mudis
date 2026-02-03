package io.mudis.mudis.client;

public interface Client {
    void connect();

    void disconnect();

    boolean isConnected();

    void send(String msg);
}
