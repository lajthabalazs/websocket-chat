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
    private final Set<MessageListener> messageListeners;
    private Game game;

    public WebsocketManagerImpl() {
        this.socketChannels = new ConcurrentHashMap<>();
        this.messageListeners = new HashSet<>();
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
     * Registers a message listener to receive player events.
     *
     * @param listener the message listener to register
     */
    public void addMessageListener(MessageListener listener) {
            messageListeners.add(listener);
    }
    
    /**
     * Unregisters a message listener.
     *
     * @param listener the message listener to unregister
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    /**
     * Forwards a player connected event to all registered listeners.
     *
     * @param socketId the unique identifier of the socket
     */
    private void notifyPlayerConnected(String socketId) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.playerConnected(socketId);
            } catch (Exception e) {
                logger.error("Error notifying listener of player connected: {}", socketId, e);
            }
        }
    }
    
    /**
     * Forwards a player disconnected event to all registered listeners.
     *
     * @param socketId the unique identifier of the socket
     */
    private void notifyPlayerDisconnected(String socketId) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.playerDisconnected(socketId);
            } catch (Exception e) {
                logger.error("Error notifying listener of player disconnected: {}", socketId, e);
            }
        }
    }
    
    /**
     * Forwards a player message to all registered listeners.
     *
     * @param socketId the unique identifier of the socket
     * @param request the message request
     */
    private void notifyPlayerMessage(String socketId, String request) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.handlePlayerMessage(socketId, request);
            } catch (Exception e) {
                logger.error("Error notifying listener of player message: {}", socketId, e);
            }
        }
    }
    
    /**
     * Registers a socket's channel.
     *
     * @param socketId the unique identifier of the socket
     * @param ctx the channel handler context for the socket's connection
     */
    @Override
    public void playerConnected(String userId, ChannelHandlerContext ctx) {
        socketChannels.put(userId, ctx);
        logger.info("Client connected: {} (userId: {})", ctx.channel().remoteAddress(), userId);
        notifyPlayerConnected(userId);
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
        notifyPlayerDisconnected(userId);
    }

    @Override
    public void handlePlayerMessage(String userId, String request) {
        logger.debug("Websocket manager received message from userId: {}", userId);
        notifyPlayerMessage(userId, request);
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

