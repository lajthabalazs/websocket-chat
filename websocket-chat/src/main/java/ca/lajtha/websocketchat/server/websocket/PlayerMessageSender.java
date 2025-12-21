package ca.lajtha.websocketchat.server.websocket;

/**
 * Interface for sending messages to players.
 */
public interface PlayerMessageSender {


    /**
     * Sends a message to a specific player.
     * 
     * @param playerId the unique identifier of the player
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    boolean sendToPlayer(String playerId, String message);


    /**
     * Checks if a player is connected.
     * 
     * @param playerId the unique identifier of the player
     * @return true if the player is connected, false otherwise
     */
    boolean isPlayerConnected(String playerId);
}
