package io.mudis.mudis;

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackages = {
                "io.mudis.mudis.client",
                "io.mudis.mudis.server",
                "io.mudis.mudis.command",
        })
public class MudisCLI {

    static void main(String... args) {
        new SpringApplicationBuilder()
                .sources(MudisCLI.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.CONSOLE)
                .headless(false)
                .run(args);
    }
}
