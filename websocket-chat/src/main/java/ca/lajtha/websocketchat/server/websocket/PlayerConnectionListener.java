package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;

/**
 * Interface for listening to player actions
 */
public interface PlayerConnectionListener {
    void playerConnected(String socketId);

    void playerDisconnected(String socketId);

    void handlePlayerMessage(String socketId, String request);
}
