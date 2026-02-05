package io.mudis.mudisclient;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.jline.PromptProvider;

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

    @Bean
    public PromptProvider myPromptProvider() {
        return () -> new AttributedString(
                "mudis-client:> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
        );
    }
}
