package ca.lajtha;

import com.google.inject.Inject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class WebSocketServer {
    private final ServerConfig config;
    private final WebSocketFrameHandler frameHandler;

    @Inject
    public WebSocketServer(ServerConfig config, WebSocketFrameHandler frameHandler) {
        this.config = config;
        this.frameHandler = frameHandler;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // HTTP codec for handling HTTP upgrade requests
                            pipeline.addLast(new HttpServerCodec());
                            
                            // Aggregates HTTP chunks into full requests
                            pipeline.addLast(new HttpObjectAggregator(config.getHttpMaxContentLength()));
                            
                            // Handles WebSocket handshake and frames
                            pipeline.addLast(new WebSocketServerProtocolHandler(config.getWebsocketPath()));
                            
                            // Custom handler for WebSocket messages
                            pipeline.addLast(frameHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, config.isSocketKeepalive());

            ChannelFuture future = bootstrap.bind(config.getPort()).sync();
            System.out.println("WebSocket server started on port " + config.getPort());
            System.out.println("Connect to: ws://localhost:" + config.getPort() + config.getWebsocketPath());

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}

