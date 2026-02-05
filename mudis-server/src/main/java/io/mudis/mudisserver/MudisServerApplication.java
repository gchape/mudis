package io.mudis.mudisserver;

import io.mudis.mudisserver.pubsub.PublisherRegistrar;
import io.mudis.mudisserver.server.ServerImpl;

public class MudisServerApplication {

    static void main() {
        var server = new ServerImpl();
        server.start();
        // after server stops
        server.stop();
        PublisherRegistrar.INSTANCE.shutdown();
    }
}
