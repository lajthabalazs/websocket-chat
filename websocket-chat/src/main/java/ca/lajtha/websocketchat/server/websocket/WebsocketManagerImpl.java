package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.game.Game;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of WebsocketManager that sends messages to WebSocket connections.
 */
public class WebsocketManagerImpl implements WebsocketManager, MessageSender {
    private static final Logger logger = LoggerFactory.getLogger(WebsocketManagerImpl.class);
    private final Map<String, ChannelHandlerContext> socketChannels;
    private Game game;

    public WebsocketManagerImpl() {
        this.socketChannels = new ConcurrentHashMap<>();
    }

    /**
     * Sets the game instance.
     *
     * @param game the game instance to set
     */
    public void setGame(Game game) {
        this.game = game;
    }

    
    /**
     * Registers a socket's channel.
     *
     * @param userId the unique identifier of the socket
     * @param ctx the channel handler context for the socket's connection
     */
    @Override
    public void playerConnected(String userId, ChannelHandlerContext ctx) {
        socketChannels.put(userId, ctx);
        logger.info("Client connected: {} (userId: {})", ctx.channel().remoteAddress(), userId);
        if (game != null) {
            game.handlePlayerConnected(userId);
        }
    }

    /**
     * Unregisters a socket's channel.
     *
     * @param userId the unique identifier of the socket
     */
    @Override
    public void playerDisconnected(String userId) {
        socketChannels.remove(userId);
        logger.info("Client disconnected: {} (userId: {})", userId, userId);
         if (game != null) {
            game.handlePlayerDisconnected(userId);
        }
    }

    @Override
    public void handlePlayerMessage(String userId, String message) {
        logger.debug("Websocket manager received message from {}: {}", userId, message);
         if (game != null) {
            game.handlePlayerMessage(userId, message);
        }
    }

    /**
     * Sends a message to a specific socket.
     * 
     * @param userId the unique identifier of the socket
     * @param message the message to send
     */
    @Override
    public void sendMessage(String userId, String message) {
        ChannelHandlerContext ctx = socketChannels.get(userId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
        }
    }
}

