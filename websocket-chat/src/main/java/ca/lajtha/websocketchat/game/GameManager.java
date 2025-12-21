package ca.lajtha.websocketchat.game;

import com.google.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple games and routes messages to the appropriate game based on player assignments.
 * Implements both Game and PlayerMessageSender interfaces to act as a router between players and games.
 */
public class GameManager implements Game, PlayerMessageSender {
    
    private final Map<String, Game> games;
    private final Map<String, Game> playerToGame = new ConcurrentHashMap<>();
    private final PlayerMessageSender messageSender;
    
    @Inject
    public GameManager(PlayerMessageSender messageSender) {
        this.games = new HashMap<>();
        this.messageSender = messageSender;
    }

    public void assignPlayerToGame(String playerId, String gameId) {
        removePlayerFromGame(playerId);
        Game game = games.get(gameId);
        playerToGame.put(playerId, game);
        game.handlePlayerConnected(playerId);
    }

    public void removePlayerFromGame(String playerId) {
        Game game = playerToGame.remove(playerId);
        if (game != null) {
            game.handlePlayerDisconnected(playerId);
        }
    }
    
    @Override
    public void handlePlayerMessage(String playerId, String message) {
        Game game = playerToGame.get(playerId);
        if (game != null) {
            game.handlePlayerMessage(playerId, message);
        }
    }
    
    @Override
    public void handlePlayerConnected(String playerId) {
        if (playerToGame.containsKey(playerId)) {
            Game game = playerToGame.get(playerId);
            game.handlePlayerConnected(playerId);
        }
    }
    
    @Override
    public void handlePlayerDisconnected(String playerId) {
        removePlayerFromGame(playerId);
    }
    
    @Override
    public boolean sendToPlayer(String playerId, String message) {
        return messageSender.sendToPlayer(playerId, message);
    }
}