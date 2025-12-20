package ca.lajtha.websocketchat.game.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatGame {
    final Set<String> players = new HashSet<>();
    final List<Message> messages = new ArrayList<>();

    public void addPlayer(String playerId) {
        players.add(playerId);
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
    }

    public List<String> getPlayers() {
        return players.stream().sorted().toList();
    }

    public void addMessage(String playerId, String text) {
        messages.add(new Message(playerId, text));
    }

    public List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}
