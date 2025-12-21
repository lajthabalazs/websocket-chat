package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;

/**
 * Interface for listening to player actions
 */
public interface PlayerConnectionListener {
    void playerConnected(String playerId, ChannelHandlerContext ctx);

    void playerDisconnected(String playerId);
}
