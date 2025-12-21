package ca.lajtha.websocketchat.server.websocket;

/**
 * Interface for listening to player actions
 */
public interface MessageListener {
    void playerConnected(String socketId);

    void playerDisconnected(String socketId);

    void handlePlayerMessage(String socketId, String request);
}
