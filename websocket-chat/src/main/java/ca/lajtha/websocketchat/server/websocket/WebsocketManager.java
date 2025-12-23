package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;

public interface WebsocketManager {
    void playerConnected(String userId, ChannelHandlerContext ctx);

    void playerDisconnected(String userId);

    void handlePlayerMessage(String userId, String request);
}
