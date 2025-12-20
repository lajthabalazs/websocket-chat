package ca.lajtha;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            // Handle text frames
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String request = textFrame.text();
            
            System.out.println("Received: " + request);
            
            // Echo the message back to the client
            String response = "Echo: " + request;
            ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
        } else {
            String message = "Unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
        ctx.channel().writeAndFlush(new TextWebSocketFrame("Welcome to the WebSocket server!"));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

