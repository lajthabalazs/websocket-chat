package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.PlayerConnection;
import ca.lajtha.websocketchat.game.Game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatGameController implements Game {

    private final ChatGame game;
    private final PlayerConnection playerConnection;

    public ChatGameController(ChatGame game, PlayerConnection playerConnection) {
        this.game = game;
        this.playerConnection = playerConnection;
    }

    @Override
    public void handlePlayerMessage(String playerId, String message) {

    }

    @Override
    public void onPlayerConnected(String playerId) {
        game.addPlayer(playerId);

    }

    @Override
    public void onPlayerDisconnected(String playerId) {
        game.removePlayer(playerId);
    }
}
