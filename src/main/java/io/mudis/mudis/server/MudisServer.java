package io.mudis.mudis.server;

import io.mudis.mudis.codec.MudisServerCodec;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MudisServer implements Server {
    static final Logger LOGGER;
    static final MultiThreadIoEventLoopGroup BOSS_GROUP;
    static final MultiThreadIoEventLoopGroup WORKER_GROUP;

    static {
        LOGGER = LoggerFactory.getLogger(MudisServer.class);

        BOSS_GROUP = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        WORKER_GROUP = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
    }

    @Value("${mudis.server.port}")
    private int port;
    @Value("${mudis.server.host}")
    private String host;
    private ServerSocketChannel server;

    @Override
    public void start() {
        var bootstrap = new ServerBootstrap();
        try {
            server = (ServerSocketChannel) bootstrap.group(BOSS_GROUP, WORKER_GROUP)
                    .channel(NioServerSocketChannel.class)
                    /* */
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    /* */
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    /* */
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new MudisServerCodec())
                                    .addLast(new MudisServerHandler());
                        }
                    })
                    .bind(host, port)
                    .sync()
                    .channel();

            LOGGER.info("Server started, listening on port {}, host {}", port, host);
            server.closeFuture()
                    .sync();
            LOGGER.info("Server stopped");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while trying to start server", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server startup interrupted", e);
        } finally {
            if (server != null) {
                server.close();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return server != null && server.isActive();
    }

    @Override
    @PreDestroy
    public void stop() {
        BOSS_GROUP.shutdownGracefully();
        WORKER_GROUP.shutdownGracefully();
    }
}
