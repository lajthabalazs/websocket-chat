package ca.lajtha.websocketchat.server.websocket;

/**
 * Interface for sending messages to players.
 */
public interface PlayerMessageSender {


    /**
     * Sends a message to a specific player.
     * 
     * @param playerId the unique identifier of the user
     * @param message the message to send
     * @return true if the message was sent, false if the player is not connected
     */
    boolean sendToPlayer(String playerId, String message);

}
