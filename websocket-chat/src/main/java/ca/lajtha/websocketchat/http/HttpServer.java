package ca.lajtha.websocketchat.http;

import ca.lajtha.websocketchat.ServerConfig;
import com.google.inject.Inject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class HttpServer {
    private final ServerConfig config;

    @Inject
    public HttpServer(ServerConfig config) {
        this.config = config;
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
                            
                            // HTTP codec for handling HTTP requests
                            pipeline.addLast(new HttpServerCodec());
                            
                            // Aggregates HTTP chunks into full requests
                            pipeline.addLast(new HttpObjectAggregator(config.getHttpMaxContentLength()));
                            
                            // Custom handler for HTTP requests
                            pipeline.addLast(new HttpRequestHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, config.getSocketBacklog())
                    .childOption(ChannelOption.SO_KEEPALIVE, config.isSocketKeepalive());

            ChannelFuture future = bootstrap.bind(config.getHttpPort()).sync();
            System.out.println("HTTP server started on port " + config.getHttpPort());
            System.out.println("Connect to: http://localhost:" + config.getHttpPort());

            future.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}


