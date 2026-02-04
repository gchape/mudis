package io.mudis.mudis.client;

import io.mudis.mudis.codec.MudisClientCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MudisClient implements Client {
    static final Logger LOGGER = LoggerFactory.getLogger(MudisClient.class);
    static final int CONNECTION_TIMEOUT_MS = 5000;

    private final MultiThreadIoEventLoopGroup WORKER_GROUP;

    @Value("${mudis.server.port}")
    private int port;
    @Value("${mudis.server.host}")
    private String host;
    private Channel channel;

    public MudisClient() {
        WORKER_GROUP = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    }

    @Override
    public void connect() {
        if (isConnected()) {
            LOGGER.warn("Already connected to {}:{}", host, port);
            return;
        }

        var bootstrap = new Bootstrap();

        try {
            channel = bootstrap.group(WORKER_GROUP)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MS)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new MudisClientCodec())
                                    .addLast(new MudisClientHandler());
                        }
                    })
                    .connect(host, port)
                    .sync()
                    .channel();

            LOGGER.info("Connected to Mudis server at {}:{}", host, port);
        } catch (InterruptedException e) {
            LOGGER.error("Connection interrupted for {}:{}", host, port, e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to server", e);
        } catch (Exception e) {
            LOGGER.error("Failed to connect to {}:{}", host, port, e);
            throw new RuntimeException("Connection failed", e);
        }
    }

    @Override
    public void send(String msg) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }

        channel.writeAndFlush(msg)
                .addListener(fut -> {
                    if (!fut.isSuccess()) {
                        LOGGER.error("Failed to send msg: {}", msg, fut.cause());
                    }
                });
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    @PreDestroy
    public void disconnect() {
        if (channel != null) {
            try {
                channel.close().sync();
                LOGGER.info("Disconnected from server");
            } catch (InterruptedException e) {
                LOGGER.error("Error during disconnect", e);
                Thread.currentThread().interrupt();
            } finally {
                WORKER_GROUP.shutdownGracefully();
            }
        }
    }
}
