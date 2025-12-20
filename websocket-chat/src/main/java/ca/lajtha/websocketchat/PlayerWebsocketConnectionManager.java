package ca.lajtha.websocketchat;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player connections and allows sending messages to players.
 */
public class PlayerWebsocketConnectionManager {
    private final Map<String, ChannelHandlerContext> playerChannels = new ConcurrentHashMap<>();

    /**
     * Registers a player's channel.
     * 
     * @param playerId the unique identifier of the player
     * @param ctx the channel handler context for the player's connection
     */
    public void registerPlayer(String playerId, ChannelHandlerContext ctx) {
        playerChannels.put(playerId, ctx);
    }

    /**
     * Unregisters a player's channel.
     * 
     * @param playerId the unique identifier of the player
     */
    public void unregisterPlayer(String playerId) {
        playerChannels.remove(playerId);
    }

    /**
     * Sends a message to a specific player.
     * 
     * @param playerId the unique identifier of the player
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    public boolean sendToPlayer(String playerId, String message) {
        ChannelHandlerContext ctx = playerChannels.get(playerId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * Gets all connected player IDs.
     * 
     * @return a set of all connected player IDs
     */
    public java.util.Set<String> getConnectedPlayers() {
        // Clean up inactive channels
        playerChannels.entrySet().removeIf(entry -> !entry.getValue().channel().isActive());
        return playerChannels.keySet();
    }

    /**
     * Checks if a player is connected.
     * 
     * @param playerId the unique identifier of the player
     * @return true if the player is connected, false otherwise
     */
    public boolean isPlayerConnected(String playerId) {
        ChannelHandlerContext ctx = playerChannels.get(playerId);
        return ctx != null && ctx.channel().isActive();
    }
}

