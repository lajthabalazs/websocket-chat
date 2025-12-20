package ca.lajtha.websocketchat.game;

/**
 * Default implementation of Game that logs all events.
 * This can be replaced with a custom implementation via dependency injection.
 */
public class VoidGame implements Game {
    
    @Override
    public void handlePlayerMessage(String playerId, String message) {
        System.out.println("Game: Player " + playerId + " sent message: " + message);
    }
    
    @Override
    public void onPlayerConnected(String playerId) {
        System.out.println("Game: Player " + playerId + " connected");
    }
    
    @Override
    public void onPlayerDisconnected(String playerId) {
        System.out.println("Game: Player " + playerId + " disconnected");
    }
}

