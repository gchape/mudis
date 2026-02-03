package io.mudis.mudis.shell;

import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

@Component
public class PubSubCommands {

    @Command
    public void sayHello() {
        System.out.println("Hello World!");
    }
}
