package io.mudis.mudisclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackages = {
                "io.mudis.mudisclient.shell",
                "io.mudis.mudisclient.queue",
                "io.mudis.mudisclient.client",
        })
public class MudisClientApplication {

    static void main(String[] args) {
        SpringApplication.run(MudisClientApplication.class, args);
    }
}
