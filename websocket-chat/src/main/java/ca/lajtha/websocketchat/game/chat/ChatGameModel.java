package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.game.chat.messages.PlayerInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatGameModel {
    final Set<String> players = new HashSet<>();
    final Map<String, String> playerScreenNames = new HashMap<>(); // playerId -> screenName
    final List<StoredMessage> messages = new ArrayList<>();
    private final List<ChatMessageListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(ChatMessageListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(ChatMessageListener listener) {
        listeners.remove(listener);
    }

    public void addPlayer(String playerId) {
        players.add(playerId);
        // Set default screen name if not already set
        if (!playerScreenNames.containsKey(playerId)) {
            playerScreenNames.put(playerId, playerId);
        }
        notifyPlayerJoined(playerId);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        notifyPlayerLeft(playerId);
    }

    public List<PlayerInfo> getPlayers() {
        return players.stream().map(playerId -> new PlayerInfo(playerId, playerScreenNames.getOrDefault(playerId, playerId))).sorted().toList();
    }

    public void addMessage(String playerId, String text) {
        StoredMessage storedMessage = new StoredMessage(playerId, text);
        messages.add(storedMessage);
        notifyMessageReceived(storedMessage);
    }

    public void setScreenName(String playerId, String screenName) {
        if (screenName == null || screenName.trim().isEmpty()) {
            throw new IllegalArgumentException("Screen name cannot be null or empty");
        }
        playerScreenNames.put(playerId, screenName);
    }

    public List<VisibleMessage> getMessages() {
        return messages.stream().map(storedMessage -> new VisibleMessage(playerScreenNames.getOrDefault(storedMessage.playerId(), storedMessage.playerId()), storedMessage.message())).toList();
    }

    private void notifyPlayerJoined(String playerId) {
        for (ChatMessageListener listener : listeners) {
            listener.onPlayerJoinedChat(playerScreenNames.getOrDefault(playerId, playerId));
        }
    }

    private void notifyPlayerLeft(String playerId) {
        for (ChatMessageListener listener : listeners) {
            listener.onPlayerLeftChat(playerScreenNames.getOrDefault(playerId, playerId));
        }
    }

    private void notifyMessageReceived(StoredMessage storedMessage) {
        VisibleMessage visibleMessage = new VisibleMessage(playerScreenNames.getOrDefault(storedMessage.playerId(), storedMessage.playerId()), storedMessage.message());
        for (ChatMessageListener listener : listeners) {

            listener.onMessageReceived(visibleMessage);
        }
    }
}
