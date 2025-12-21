package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of WebsocketManager that sends messages to WebSocket connections.
 */
public class WebsocketManagerImpl implements WebsocketManager, MessageSender {
    private final Map<String, ChannelHandlerContext> socketChannels;
    private final Set<MessageListener> messageListeners;

    public WebsocketManagerImpl() {
        this.socketChannels = new ConcurrentHashMap<>();
        this.messageListeners = new HashSet<>();
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
                System.err.println("Error notifying listener of player connected: " + e.getMessage());
                e.printStackTrace();
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
                System.err.println("Error notifying listener of player disconnected: " + e.getMessage());
                e.printStackTrace();
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
                System.err.println("Error notifying listener of player message: " + e.getMessage());
                e.printStackTrace();
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
    public void playerConnected(String socketId, ChannelHandlerContext ctx) {
        socketChannels.put(socketId, ctx);
        notifyPlayerConnected(socketId);
    }

    /**
     * Unregisters a socket's channel.
     *
     * @param socketId the unique identifier of the socket
     */
    @Override
    public void playerDisconnected(String socketId) {
        socketChannels.remove(socketId);
        notifyPlayerDisconnected(socketId);
    }

    @Override
    public void handlePlayerMessage(String socketId, String request) {
        notifyPlayerMessage(socketId, request);
    }

    /**
     * Sends a message to a specific socket.
     * 
     * @param socketId the unique identifier of the socket
     * @param message the message to send
     */
    @Override
    public void sendMessage(String socketId, String message) {
        ChannelHandlerContext ctx = socketChannels.get(socketId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
        }
    }
}

