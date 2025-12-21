package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.game.Game;
import com.google.inject.Inject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;

import java.util.UUID;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final AttributeKey<String> SOCKET_ID_KEY = AttributeKey.valueOf("socketId");

    private final PlayerConnectionListener playerConnectionListener;
    private final WebsocketManager websocketManager;

    public WebSocketFrameHandler(PlayerConnectionListener playerConnectionListener, WebsocketManager websocketManager) {
        this.playerConnectionListener = playerConnectionListener;
        this.websocketManager = websocketManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            // Handle text frames
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String request = textFrame.text();
            
            // Get the socketId from channel attributes
            String socketId = ctx.channel().attr(SOCKET_ID_KEY).get();
            if (socketId == null) {
                System.err.println("Warning: Received message from channel without socketId");
                return;
            }
            
            System.out.println("Received from socket " + socketId + ": " + request);
            
            // Forward message to game
            playerConnectionListener.handlePlayerMessage(socketId, request);
        } else {
            String message = "Unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Generate a unique socketId for this connection
        String socketId = UUID.randomUUID().toString();
        ctx.channel().attr(SOCKET_ID_KEY).set(socketId);
        
        System.out.println("Client connected: " + ctx.channel().remoteAddress() + " (socketId: " + socketId + ")");
        playerConnectionListener.playerConnected(socketId);
        websocketManager.playerConnected(socketId, ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Get the socketId from channel attributes
        String socketId = ctx.channel().attr(SOCKET_ID_KEY).get();
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress() + " (socketId: " + socketId + ")");
        playerConnectionListener.playerDisconnected(socketId);
        websocketManager.playerDisconnected(socketId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

