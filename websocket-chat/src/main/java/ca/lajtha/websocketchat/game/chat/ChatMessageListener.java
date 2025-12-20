package ca.lajtha.websocketchat.game.chat;

/**
 * Listener interface for chat game events.
 * Implementations of this interface will be notified when players join, leave, or send messages.
 */
public interface ChatMessageListener {

    /**
     * Called when a new player joins the chat game.
     *
     * @param screenName the unique identifier of the player who joined
     */
    void onPlayerJoinedChat(String screenName);

    /**
     * Called when a player leaves the chat game.
     *
     * @param screenName the unique identifier of the player who left
     */
    void onPlayerLeftChat(String screenName);

    /**
     * Called when a new message is received from a player.
     *
     * @param visibleMessage The new message
     */
    void onMessageReceived(VisibleMessage visibleMessage);
}
