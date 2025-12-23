package ca.lajtha.websocketchat.game;

import ca.lajtha.websocketchat.game.chat.ChatGame;
import ca.lajtha.websocketchat.game.chat.ChatGameModel;
import ca.lajtha.websocketchat.server.websocket.MessageSender;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages multiple games and routes messages to the appropriate game based on player assignments.
 * Implements both Game and PlayerMessageSender interfaces to act as a router between players and games.
 * Created via factory method in ServerModule to handle circular dependencies.
 */
public class GameManager implements Game, MessageSender {
    
    private final Map<String, SerializedGame> games;
    private final Map<String, GameInfo> gameInfoMap;
    private final Map<String, Game> playerToGame = new ConcurrentHashMap<>();
    private final MessageSender messageSender;
    private int gameIdCounter = 1;
    
    public GameManager(MessageSender messageSender) {
        this.games = new ConcurrentHashMap<>();
        this.gameInfoMap = new ConcurrentHashMap<>();
        this.messageSender = messageSender;
    }

    /**
     * Creates a new game and returns its ID.
     * 
     * @param playerId the ID of the player creating the game
     * @param gameParameters parameters for the game (currently unused, reserved for future use)
     * @return the unique game ID
     */
    public String createGame(String playerId, Map<String, Object> gameParameters) {
        String gameId = "game-" + gameIdCounter++;
        ChatGameModel gameModel = new ChatGameModel();
        ChatGame chatGame = new ChatGame(gameModel, messageSender);
        
        // Wrap the game with SerializedGame to ensure sequential processing
        SerializedGame serializedGame = new SerializedGame(gameId, chatGame);
        games.put(gameId, serializedGame);
        
        // Store game info for listing
        String gameName = gameParameters != null && gameParameters.containsKey("name") 
            ? (String) gameParameters.get("name") 
            : "Game " + gameId;
        gameInfoMap.put(gameId, new GameInfo(gameId, gameName, playerId, new Date()));
        return gameId;
    }

    /**
     * Joins a player to a game.
     *
     * @param playerId the ID of the player joining
     * @param gameId the ID of the game to join
     * @throws IllegalArgumentException if the game does not exist
     */
    public void joinGame(String playerId, String gameId) {
        Game game = games.get(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game with ID " + gameId + " does not exist");
        }
        assignPlayerToGame(playerId, gameId);
    }

    /**
     * Lists all available games with their descriptions.
     * 
     * @return list of game descriptions
     */
    public List<GameInfo> listGames() {
        return new ArrayList<>(gameInfoMap.values());
    }

    /**
     * Stops a game and disconnects all players.
     * 
     * @param gameId the ID of the game to stop
     * @throws IllegalArgumentException if the game does not exist
     */
    public void stopGame(String gameId) {
        SerializedGame game = games.remove(gameId);
        if (game == null) {
            throw new IllegalArgumentException("Game with ID " + gameId + " does not exist");
        }
        
        // Disconnect all players from this game
        List<String> playersToRemove = playerToGame.entrySet().stream()
            .filter(entry -> entry.getValue() == game)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String playerId : playersToRemove) {
            removePlayerFromGame(playerId);
        }


            // Give up to 5 seconds for pending tasks to complete
            boolean shutdown = game.shutdown(5000);
            if (!shutdown) {
                System.err.println("Warning: SerializedGame executor for " + gameId + " did not shutdown gracefully, forcing shutdown");
                game.shutdownNow();
            }
        
        // Remove the game

        gameInfoMap.remove(gameId);
    }

    private void assignPlayerToGame(String playerId, String gameId) {
        removePlayerFromGame(playerId);
        Game game = games.get(gameId);
        if (game != null) {
            playerToGame.put(playerId, game);
            game.handlePlayerConnected(playerId);
        }
    }

    private void removePlayerFromGame(String playerId) {
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
    public void sendMessage(String playerId, String message) {
        messageSender.sendMessage(playerId, message);
    }
}