package io.mudis.mudis;

import io.mudis.mudis.server.Server;
import io.mudis.mudis.shell.KVCommands;
import io.mudis.mudis.shell.PubSubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.shell.core.ShellRunner;
import org.springframework.shell.core.command.annotation.EnableCommand;

@ComponentScan(basePackages = {
        "io.mudis.mudis.shell",
        "io.mudis.mudis.server.config"
})
@EnableCommand(value = {
        KVCommands.class,
        PubSubCommands.class,
})
public class MudisApplication {

    private static final Logger LOGGER;

    static {
        LOGGER = LoggerFactory.getLogger(MudisApplication.class);
    }

    static void main(String... args) throws Exception {
        var context = new SpringApplicationBuilder()
                .sources(MudisApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .run(args);

        var shellRunner = context.getBean(ShellRunner.class);
        var server = context.getBean(Server.class);
        var env = context.getEnvironment();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(server::stop));

        startServer(
                env.getProperty("mudis.server.host"),
                env.getProperty("mudis.server.port"),
                server);

        shellRunner.run(args);
    }

    private static void startServer(String host,
                                    String port,
                                    Server server) {
        Thread.ofPlatform()
                .name("mudis-server")
                .uncaughtExceptionHandler((_, throwable) -> {
                    LOGGER.error(throwable.getMessage());
                    System.exit(-1);
                })
                .start(() -> server.start(host, Integer.parseInt(port)));
    }
}
