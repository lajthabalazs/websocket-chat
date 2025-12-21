package ca.lajtha.websocketchat.server.websocket;

import io.netty.channel.ChannelHandlerContext;

public interface WebsocketManager {
    void playerConnected(String socketId, ChannelHandlerContext ctx);

    void playerDisconnected(String socketId);

    void handlePlayerMessage(String socketId, String request);
}
