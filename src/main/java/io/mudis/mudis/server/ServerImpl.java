package io.mudis.mudis.server;

import io.mudis.mudis.codec.ServerCodec;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ServerImpl implements Server {
    private static final Logger Log = LoggerFactory.getLogger(ServerImpl.class);

    private final MultiThreadIoEventLoopGroup bossGroup;
    private final MultiThreadIoEventLoopGroup workerGroup;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${mudis.server.port:6379}")
    private int port;

    @Value("${mudis.server.host:0.0.0.0}")
    private String host;

    private volatile ServerSocketChannel serverChannel;

    public ServerImpl() {
        this.bossGroup = new MultiThreadIoEventLoopGroup(4, NioIoHandler.newFactory());
        this.workerGroup = new MultiThreadIoEventLoopGroup(8, NioIoHandler.newFactory());
    }

    @Override
    public void start() {
        if (running.getAndSet(true)) {
            Log.warn("Server is already running");
            return;
        }

        ServerBootstrap bootstrap = new ServerBootstrap();
        try {
            Log.info("Starting Mudis server on {}:{}", host, port);

            serverChannel = (ServerSocketChannel) bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new ServerCodec())
                                    .addLast(new ServerHandler());
                        }
                    })
                    .bind(host, port)
                    .sync()
                    .channel();

            Log.info("Mudis server started successfully on {}:{}", host, port);
            serverChannel.closeFuture().sync();

        } catch (InterruptedException e) {
            Log.error("Server startup interrupted", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Server startup interrupted", e);
        } catch (Exception e) {
            Log.error("Failed to start server", e);
            throw new RuntimeException("Failed to start server", e);
        } finally {
            running.set(false);
            Log.info("Server stopped");
        }
    }

    @Override
    public boolean isRunning() {
        return running.get() && serverChannel != null && serverChannel.isActive();
    }

    @Override
    public void stop() {
        if (!running.get()) {
            Log.debug("Server is not running");
            return;
        }

        Log.info("Stopping Mudis server...");

        if (serverChannel != null && serverChannel.isActive()) {
            try {
                serverChannel.close().sync();
                Log.info("Server channel closed");
            } catch (InterruptedException e) {
                Log.error("Error closing server channel", e);
                Thread.currentThread().interrupt();
            }
        }

        try {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            Log.info("Event loop groups shut down");
        } catch (InterruptedException e) {
            Log.error("Error shutting down event loop groups", e);
            Thread.currentThread().interrupt();
        }

        running.set(false);
        Log.info("Mudis server stopped successfully");
    }
}
