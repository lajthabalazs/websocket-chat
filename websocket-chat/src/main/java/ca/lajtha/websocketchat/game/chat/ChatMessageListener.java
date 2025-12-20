package ca.lajtha.websocketchat.game.chat;

/**
 * Listener interface for chat game events.
 * Implementations of this interface will be notified when players join, leave, or send messages.
 */
public interface ChatMessageListener {

    /**
     * Called when a new player joins the chat game.
     *
     * @param playerId the unique identifier of the player who joined
     */
    void onPlayerJoinedChat(String playerId);

    /**
     * Called when a player leaves the chat game.
     *
     * @param playerId the unique identifier of the player who left
     */
    void onPlayerLeftChat(String playerId);

    /**
     * Called when a new message is received from a player.
     *
     * @param playerId the unique identifier of the player who sent the message
     * @param message the message text that was sent
     */
    void onMessageReceived(String playerId, String message);
}
