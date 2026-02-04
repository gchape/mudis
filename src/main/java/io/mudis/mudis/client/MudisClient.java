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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class MudisClient implements Client {
    private static final Logger LOGGER = LoggerFactory.getLogger(MudisClient.class);
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private final MultiThreadIoEventLoopGroup workerGroup;

    @Value("${mudis.server.port:6379}")
    private int port;

    @Value("${mudis.server.host:localhost}")
    private String host;

    private volatile Channel channel;

    public MudisClient() {
        this.workerGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    }

    @Override
    public void connect() {
        if (isConnected()) {
            LOGGER.warn("Already connected to {}:{}", host, port);
            return;
        }

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                LOGGER.info("Connecting to {}:{} (attempt {}/{})", host, port, attempt, MAX_RETRY_ATTEMPTS);

                Bootstrap bootstrap = new Bootstrap();
                channel = bootstrap.group(workerGroup)
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
                return;

            } catch (InterruptedException e) {
                LOGGER.error("Connection interrupted", e);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to connect to server", e);
            } catch (Exception e) {
                lastException = e;
                LOGGER.warn("Connection attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Connection retry interrupted", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Failed to connect after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    @Override
    public void send(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }

        channel.writeAndFlush(msg)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        LOGGER.error("Failed to send message: {}", msg, future.cause());
                    }
                });
    }

    @Override
    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @Override
    public void disconnect() {
        if (channel != null) {
            try {
                channel.close().sync();
                LOGGER.info("Disconnected from server");
            } catch (InterruptedException e) {
                LOGGER.error("Error during disconnect", e);
                Thread.currentThread().interrupt();
            } finally {
                try {
                    workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS).sync();
                    LOGGER.info("Worker group shut down");
                } catch (InterruptedException e) {
                    LOGGER.error("Error shutting down worker group", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
