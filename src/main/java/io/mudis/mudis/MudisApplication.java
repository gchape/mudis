package io.mudis.mudis;

import io.mudis.mudis.shell.KVCommands;
import io.mudis.mudis.shell.PubSubCommands;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.annotation.EnableCommand;

@ComponentScan(basePackages = {
        "io.mudis.mudis.shell"
})
@EnableCommand(value = {
        KVCommands.class,
        PubSubCommands.class,
})
public class MudisApplication {

    static void main(String... args) throws Exception {
        var context = new AnnotationConfigApplicationContext(MudisApplication.class);
        var shellRunner = context.getBean(ShellRunner.class);

        shellRunner.run(args);
        context.close();
    }
}
