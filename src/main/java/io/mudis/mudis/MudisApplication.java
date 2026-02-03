package io.mudis.mudis;

import io.mudis.mudis.server.Server;
import io.mudis.mudis.shell.KVCommands;
import io.mudis.mudis.shell.PubSubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.annotation.EnableCommand;

import java.util.Objects;

@ComponentScan(basePackages = {
        "io.mudis.mudis.shell",
        "io.mudis.mudis.server.config"
})
@EnableCommand(value = {
        KVCommands.class,
        PubSubCommands.class,
})
public class MudisApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MudisApplication.class);

    static void main(String... args) throws Exception {
        var context = createApplicationContext(args);
        var server = context.getBean(Server.class);

        registerShutdownHook(server);
        startMudisServer(context.getEnvironment(), server);
        runShell(context, args);
    }

    private static ConfigurableApplicationContext createApplicationContext(String[] args) {
        return new SpringApplicationBuilder()
                .sources(MudisApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);
    }

    private static void registerShutdownHook(Server server) {
        Runtime.getRuntime()
                .addShutdownHook(new Thread(server::stop));
    }

    private static void startMudisServer(Environment env, Server server) {
        var host = Objects.requireNonNull(env.getProperty("mudis.server.host"));
        var port = Objects.requireNonNull(env.getProperty("mudis.server.port"));

        Thread.ofPlatform()
                .name("mudis-server")
                .uncaughtExceptionHandler(createServerExceptionHandler())
                .start(() -> server.start(host, Integer.parseInt(port)));
    }

    private static Thread.UncaughtExceptionHandler createServerExceptionHandler() {
        return (_, throwable) -> {
            LOGGER.error("Server failed to start", throwable);
            System.exit(-1);
        };
    }

    private static void runShell(ConfigurableApplicationContext context, String[] args) throws Exception {
        var shellRunner = context.getBean(ShellRunner.class);
        shellRunner.run(args);
    }
}
