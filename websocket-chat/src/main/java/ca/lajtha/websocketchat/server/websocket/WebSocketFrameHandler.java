package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    private static final AttributeKey<String> SOCKET_ID_KEY = AttributeKey.valueOf("socketId");
    private static final AttributeKey<Boolean> HANDSHAKE_COMPLETE_KEY = AttributeKey.valueOf("handshakeComplete");
    // Use the same USER_ID_KEY as WebSocketHandshakeHandler
    private static final AttributeKey<String> USER_ID_KEY = WebSocketHandshakeHandler.getUserIdKey();

    private final WebsocketManager websocketManager;

    public WebSocketFrameHandler(WebsocketManager websocketManager) {
        this.websocketManager = websocketManager;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        logger.debug("WebSocketFrameHandler received user event: {}", evt.getClass().getName());
        
        // This is called when the WebSocket handshake is complete
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            logger.debug("WebSocket handshake completion event received");
            
            // Verify that the connection was authenticated during handshake
            String userId = ctx.channel().attr(USER_ID_KEY).get();
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("WebSocket connection rejected: No authenticated userId found. Connection from: {}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
            
            // Generate a unique socketId for this connection
            String socketId = UUID.randomUUID().toString();
            ctx.channel().attr(SOCKET_ID_KEY).set(socketId);
            ctx.channel().attr(HANDSHAKE_COMPLETE_KEY).set(true);
            
            logger.info("Client connected: {} (socketId: {}, userId: {})", ctx.channel().remoteAddress(), socketId, userId);
            websocketManager.playerConnected(userId, ctx);
        } else {
            logger.debug("Received non-handshake event: {}", evt.getClass().getName());
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // Verify handshake completed
        Boolean handshakeComplete = ctx.channel().attr(HANDSHAKE_COMPLETE_KEY).get();
        if (handshakeComplete == null || !handshakeComplete) {
            logger.warn("Warning: Received frame before handshake completed. Closing connection.");
            ctx.close();
            return;
        }
        
        // Verify authentication before processing any frames
        String userId = ctx.channel().attr(USER_ID_KEY).get();
        if (userId == null || userId.isEmpty()) {
            logger.warn("Warning: Received frame from unauthenticated connection. Closing connection.");
            ctx.close();
            return;
        }
        
        if (frame instanceof TextWebSocketFrame) {
            // Handle text frames
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            String request = textFrame.text();
            
            // Get the socketId from channel attributes
            String socketId = ctx.channel().attr(SOCKET_ID_KEY).get();
            if (socketId == null) {
                logger.warn("Warning: Received message from channel without socketId");
                return;
            }
            
            logger.debug("Received from socket {} (userId: {}): {}", socketId, userId, request);
            
            // Forward message to game
            websocketManager.handlePlayerMessage(userId, request);
        } else {
            String message = "Unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Get the socketId from channel attributes
        String socketId = ctx.channel().attr(SOCKET_ID_KEY).get();
        String userId = ctx.channel().attr(USER_ID_KEY).get();
        Boolean handshakeComplete = ctx.channel().attr(HANDSHAKE_COMPLETE_KEY).get();
        
        if (socketId != null) {
            logger.info("Client disconnected: {} (socketId: {}, userId: {})", ctx.channel().remoteAddress(), socketId, userId);
            websocketManager.playerDisconnected(userId);
        } else {
            logger.debug("Client disconnected before handshake completed: {} (userId: {}, handshakeComplete: {})", 
                    ctx.channel().remoteAddress(), userId, handshakeComplete);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught in WebSocketFrameHandler", cause);
        ctx.close();
    }
}

