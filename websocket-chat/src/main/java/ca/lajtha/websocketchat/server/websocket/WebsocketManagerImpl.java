package ca.lajtha.websocketchat.server.websocket;

import com.google.inject.Inject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;

/**
 * Implementation of WebsocketManager that sends messages to WebSocket connections.
 */
public class WebsocketManagerImpl implements WebsocketManager {
    private final Map<String, ChannelHandlerContext> socketChannels;

    /**
     * Creates a new WebsocketManagerImpl with the given socket channels map.
     * 
     * @param socketChannels the map of socket IDs to their channel handler contexts
     */
    @Inject
    WebsocketManagerImpl(Map<String, ChannelHandlerContext> socketChannels) {
        this.socketChannels = socketChannels;
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
    }

    /**
     * Unregisters a socket's channel.
     *
     * @param socketId the unique identifier of the socket
     */
    @Override
    public void playerDisconnected(String socketId) {
        socketChannels.remove(socketId);
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

