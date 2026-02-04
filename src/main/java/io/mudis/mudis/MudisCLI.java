package io.mudis.mudis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackages = {
                "io.mudis.mudis.client",
                "io.mudis.mudis.server",
                "io.mudis.mudis.command",
        })
public class MudisCLI {

    static void main(String... args) {
        SpringApplication.run(MudisCLI.class, args);
    }
}
