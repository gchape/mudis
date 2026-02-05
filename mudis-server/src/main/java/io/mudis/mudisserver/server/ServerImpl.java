package io.mudis.mudisserver.server;

import io.mudis.mudisserver.utils.ConfigProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Netty-based server implementation for Mudis pub/sub system.
 */
public class ServerImpl implements Server {
    private static final Logger Log = LoggerFactory.getLogger(ServerImpl.class);
    private static final int BOSS_THREADS = 1;
    private static final int WORKER_THREADS = 4;

    private final MultiThreadIoEventLoopGroup bossGroup;
    private final MultiThreadIoEventLoopGroup workerGroup;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private final int port;
    private final String host;

    private volatile ServerSocketChannel serverChannel;

    public ServerImpl() {
        this.host = ConfigProperties.get("mudis.server.host");
        this.port = Integer.parseInt(ConfigProperties.get("mudis.server.port"));
        this.bossGroup = new MultiThreadIoEventLoopGroup(BOSS_THREADS, NioIoHandler.newFactory());
        this.workerGroup = new MultiThreadIoEventLoopGroup(WORKER_THREADS, NioIoHandler.newFactory());
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            Log.warn("Server is already running");
            return;
        }

        try {
            Log.info("Starting Mudis server on {}:{}", host, port);

            ServerBootstrap bootstrap = new ServerBootstrap();
            ChannelFuture future = bootstrap
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
                                    .addLast(new io.mudis.mudisserver.codec.ServerCodec())
                                    .addLast(new ServerHandler());
                        }
                    })
                    .bind(host, port)
                    .sync();

            serverChannel = (ServerSocketChannel) future.channel();
            Log.info("Mudis server started successfully on {}:{}", host, port);

            serverChannel.closeFuture().sync();

        } catch (InterruptedException e) {
            Log.error("Server startup interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            Log.error("Failed to start server", e);
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

        closeServerChannel();
        shutdownEventLoops();

        running.set(false);
        Log.info("Mudis server stopped successfully");
    }

    private void closeServerChannel() {
        if (serverChannel != null && serverChannel.isActive()) {
            try {
                serverChannel.close().sync();
                Log.info("Server channel closed");
            } catch (InterruptedException e) {
                Log.error("Error closing server channel", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void shutdownEventLoops() {
        try {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
            Log.info("Event loop groups shut down");
        } catch (InterruptedException e) {
            Log.error("Error shutting down event loop groups", e);
            Thread.currentThread().interrupt();
        }
    }
}
