package io.mudis.mudis.command;

import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
public class KVCommands {

    @Command(name = "set", description = "Set a key-value pair")
    public String set(@Option(required = true) String key,
                      @Option(required = true) String value) {
        return "OK";
    }

    @Command(name = "get", description = "Get a value by key")
    public String get(@Option(required = true) String key) {
        return "value";
    }
}
