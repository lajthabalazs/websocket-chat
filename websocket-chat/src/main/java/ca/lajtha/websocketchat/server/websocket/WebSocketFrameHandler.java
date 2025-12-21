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
    private static final AttributeKey<String> PLAYER_ID_KEY = AttributeKey.valueOf("playerId");

    private final PlayerConnectionListener playerConnectionListener;

    public WebSocketFrameHandler(PlayerConnectionListener playerConnectionListener) {
        this.playerConnectionListener = playerConnectionListener;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            // Handle text frames
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String request = textFrame.text();
            
            // Get the playerId from channel attributes
            String playerId = ctx.channel().attr(PLAYER_ID_KEY).get();
            if (playerId == null) {
                System.err.println("Warning: Received message from channel without playerId");
                return;
            }
            
            System.out.println("Received from player " + playerId + ": " + request);
            
            // Forward message to game
            playerConnectionListener.handlePlayerMessage(playerId, request);
        } else {
            String message = "Unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // Generate a unique playerId for this connection
        String playerId = UUID.randomUUID().toString();
        ctx.channel().attr(PLAYER_ID_KEY).set(playerId);
        
        System.out.println("Client connected: " + ctx.channel().remoteAddress() + " (playerId: " + playerId + ")");
        playerConnectionListener.playerConnected(playerId, ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Get the playerId from channel attributes
        String playerId = ctx.channel().attr(PLAYER_ID_KEY).get();
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress() + " (playerId: " + playerId + ")");
        playerConnectionListener.playerDisconnected(playerId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Exception caught: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}

