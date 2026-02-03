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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum MudisServer implements Server {
    INSTANCE;

    static final Logger LOGGER;
    static final MultiThreadIoEventLoopGroup bossGroup;
    static final MultiThreadIoEventLoopGroup workerGroup;

    static {
        LOGGER = LoggerFactory.getLogger(MudisServer.class);

        bossGroup = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(2, NioIoHandler.newFactory());
    }

    private ServerSocketChannel server;

    @Override
    public void start(String host, int port) {
        var bootstrap = new ServerBootstrap();
        try {
            server = (ServerSocketChannel) bootstrap.group(bossGroup, workerGroup)
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
                            socketChannel.pipeline().addLast(new MudisServerCodec());
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
        return server.isActive();
    }

    @Override
    public void stop() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
