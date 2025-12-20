package ca.lajtha.websocketchat.game;

public interface Game {
    /**
     * Handles a message from a player.
     * 
     * @param playerId the unique identifier of the player sending the message
     * @param message the message content from the player
     */
    void handlePlayerMessage(String playerId, String message);
    
    /**
     * Called when a player connects to the game.
     * 
     * @param playerId the unique identifier of the player that connected
     */
    void onPlayerConnected(String playerId);
    
    /**
     * Called when a player disconnects from the game.
     * 
     * @param playerId the unique identifier of the player that disconnected
     */
    void onPlayerDisconnected(String playerId);
}

