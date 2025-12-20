package ca.lajtha.websocketchat.game.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatGame {
    final Set<String> players = new HashSet<>();
    final List<Message> messages = new ArrayList<>();
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
        notifyPlayerJoined(playerId);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        notifyPlayerLeft(playerId);
    }

    public List<String> getPlayers() {
        return players.stream().sorted().toList();
    }

    public void addMessage(String playerId, String text) {
        messages.add(new Message(playerId, text));
        notifyMessageReceived(playerId, text);
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }

    private void notifyPlayerJoined(String playerId) {
        for (ChatMessageListener listener : listeners) {
            listener.onPlayerJoinedChat(playerId);
        }
    }

    private void notifyPlayerLeft(String playerId) {
        for (ChatMessageListener listener : listeners) {
            listener.onPlayerLeftChat(playerId);
        }
    }

    private void notifyMessageReceived(String playerId, String message) {
        for (ChatMessageListener listener : listeners) {
            listener.onMessageReceived(playerId, message);
        }
    }
}
