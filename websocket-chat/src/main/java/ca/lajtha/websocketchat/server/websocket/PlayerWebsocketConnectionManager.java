package ca.lajtha.websocketchat.server.websocket;

import ca.lajtha.websocketchat.game.Game;
import com.google.inject.Inject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player connections and allows sending messages to players.
 */
public class PlayerWebsocketConnectionManager implements PlayerConnectionListener, PlayerMessageSender {
    private final Map<String, ChannelHandlerContext> playerChannels = new ConcurrentHashMap<>();
    private final Game game;

    @Inject
    PlayerWebsocketConnectionManager(Game game) {
        this.game = game;
    }
    /**
     * Registers a player's channel.
     * 
     * @param playerId the unique identifier of the player
     * @param ctx the channel handler context for the player's connection
     */
    @Override
    public void playerConnected(String playerId, ChannelHandlerContext ctx) {
        playerChannels.put(playerId, ctx);
    }

    /**
     * Unregisters a player's channel.
     * 
     * @param playerId the unique identifier of the player
     */
    @Override
    public void playerDisconnected(String playerId) {
        playerChannels.remove(playerId);
    }

    @Override
    public void handlePlayerMessage(String playerId, String request) {
        game.handlePlayerMessage(playerId, request);
    }

    /**
     * Sends a message to a specific player.
     * 
     * @param playerId the unique identifier of the player
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    @Override
    public boolean sendToPlayer(String playerId, String message) {
        ChannelHandlerContext ctx = playerChannels.get(playerId);
        if (ctx != null && ctx.channel().isActive()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(message));
            return true;
        }
        return false;
    }

    /**
     * Checks if a player is connected.
     * 
     * @param playerId the unique identifier of the player
     * @return true if the player is connected, false otherwise
     */
    @Override
    public boolean isPlayerConnected(String playerId) {
        ChannelHandlerContext ctx = playerChannels.get(playerId);
        return ctx != null && ctx.channel().isActive();
    }
}

