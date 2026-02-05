package io.mudis.mudisclient.client;

public interface Client {
    void connect();

    void disconnect();

    boolean isConnected();

    void send(String msg);
}
