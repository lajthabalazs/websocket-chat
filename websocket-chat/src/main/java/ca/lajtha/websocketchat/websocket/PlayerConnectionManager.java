package ca.lajtha.websocketchat.websocket;

import io.netty.channel.ChannelHandlerContext;

public interface PlayerConnectionManager extends PlayerConnection {
    void registerPlayer(String playerId, ChannelHandlerContext ctx);

    void unregisterPlayer(String playerId);

    java.util.Set<String> getConnectedPlayers();
}
