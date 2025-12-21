package ca.lajtha.websocketchat.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VoidGameTest {

    private VoidGame voidGame;

    @BeforeEach
    void setUp() {
        voidGame = new VoidGame();
    }

    @Test
    void handlePlayerMessage_doesNotThrow() {
        // Arrange
        String playerId = "player-123";
        String message = "Hello, game!";

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            voidGame.handlePlayerMessage(playerId, message);
        });
    }

    @Test
    void handlePlayerMessage_withNullPlayerId() {
        // Arrange
        String message = "Test message";

        // Act & Assert - should not throw even with null playerId
        assertDoesNotThrow(() -> {
            voidGame.handlePlayerMessage(null, message);
        });
    }

    @Test
    void handlePlayerMessage_withNullMessage() {
        // Arrange
        String playerId = "player-123";

        // Act & Assert - should not throw even with null message
        assertDoesNotThrow(() -> {
            voidGame.handlePlayerMessage(playerId, null);
        });
    }

    @Test
    void onPlayerConnected_doesNotThrow() {
        // Arrange
        String playerId = "player-123";

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            voidGame.onPlayerConnected(playerId);
        });
    }

    @Test
    void onPlayerConnected_withNullPlayerId() {
        // Act & Assert - should not throw even with null playerId
        assertDoesNotThrow(() -> {
            voidGame.onPlayerConnected(null);
        });
    }

    @Test
    void onPlayerDisconnected_doesNotThrow() {
        // Arrange
        String playerId = "player-123";

        // Act & Assert - should not throw
        assertDoesNotThrow(() -> {
            voidGame.onPlayerDisconnected(playerId);
        });
    }

    @Test
    void onPlayerDisconnected_withNullPlayerId() {
        // Act & Assert - should not throw even with null playerId
        assertDoesNotThrow(() -> {
            voidGame.onPlayerDisconnected(null);
        });
    }
}


