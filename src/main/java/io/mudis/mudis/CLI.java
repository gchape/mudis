package io.mudis.mudis;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.jline.PromptProvider;

@SpringBootApplication(
        proxyBeanMethods = false,
        scanBasePackages = {
                "io.mudis.mudis.client",
                "io.mudis.mudis.server",
                "io.mudis.mudis.shell",
                "io.mudis.mudis.mq",
                "io.mudis.mudis.pubsub"
        })
public class CLI {

    static void main(String... args) {
        SpringApplication.run(CLI.class, args);
    }

    @Bean
    public PromptProvider myPromptProvider() {
        return () -> new AttributedString(
                "mudis:> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
        );
    }
}
