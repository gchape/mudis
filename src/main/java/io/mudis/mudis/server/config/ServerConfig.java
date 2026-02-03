package io.mudis.mudis.server.config;

import io.mudis.mudis.server.MudisServer;
import io.mudis.mudis.server.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ServerConfig {

    @Bean
    public Server server() {
        return MudisServer.INSTANCE;
    }
}
