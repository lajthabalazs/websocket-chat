package ca.lajtha.websocketchat.game;

import java.util.concurrent.*;

/**
 * Wraps a Game instance to ensure all operations are processed sequentially,
 * eliminating race conditions and parallel execution issues.
 * 
 * Each game gets its own single-threaded executor that processes all game
 * operations (handlePlayerMessage, handlePlayerConnected, handlePlayerDisconnected)
 * in a serialized manner.
 */
public class SerializedGame implements Game {
    
    private final Game delegate;
    private final ExecutorService executor;
    private final String gameId;
    
    /**
     * Creates a new SerializedGame wrapper around the given game.
     * 
     * @param gameId the unique identifier for this game (used for thread naming)
     * @param delegate the game instance to wrap
     */
    public SerializedGame(String gameId, Game delegate) {
        this.gameId = gameId;
        this.delegate = delegate;
        // Create a single-threaded executor for this game
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "GameExecutor-" + gameId);
            thread.setDaemon(true);
            return thread;
        });
    }
    
    /**
     * Shuts down the executor and waits for pending tasks to complete.
     * Should be called when the game is being stopped/destroyed.
     * 
     * @param timeoutMs maximum time to wait for shutdown in milliseconds
     * @return true if shutdown completed within the timeout, false otherwise
     */
    public boolean shutdown(long timeoutMs) {
        executor.shutdown();
        try {
            return executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            return false;
        }
    }
    
    /**
     * Forcefully shuts down the executor, interrupting any running tasks.
     * Should only be used if graceful shutdown fails.
     */
    public void shutdownNow() {
        executor.shutdownNow();
    }
    
    @Override
    public void handlePlayerMessage(String playerId, String message) {
        executor.execute(() -> {
            try {
                delegate.handlePlayerMessage(playerId, message);
            } catch (Exception e) {
                System.err.println("Error processing player message in game " + gameId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public void handlePlayerConnected(String playerId) {
        executor.execute(() -> {
            try {
                delegate.handlePlayerConnected(playerId);
            } catch (Exception e) {
                System.err.println("Error processing player connection in game " + gameId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public void handlePlayerDisconnected(String playerId) {
        executor.execute(() -> {
            try {
                delegate.handlePlayerDisconnected(playerId);
            } catch (Exception e) {
                System.err.println("Error processing player disconnection in game " + gameId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}




