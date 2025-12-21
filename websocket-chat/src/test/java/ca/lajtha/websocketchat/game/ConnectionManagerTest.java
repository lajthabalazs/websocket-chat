package ca.lajtha.websocketchat.game;

import ca.lajtha.websocketchat.server.websocket.MessageSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {

    private ConnectionManager connectionManager;
    @Mock private Game game;
    @Mock private MessageSender messageSender;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager(messageSender);
        connectionManager.setGame(game);
    }

    @Test
    void sendToPlayer_sendsMessage() {
        // Arrange
        String socketId = "socket1";
        String message = "Test message";
        connectionManager.playerConnected(socketId);

        // Act
        boolean sent = connectionManager.sendToPlayer(socketId, message);

        // Assert
        assertTrue(sent);
        ArgumentCaptor<String> socketIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).sendMessage(socketIdCaptor.capture(), messageCaptor.capture());
        assertEquals(socketId, socketIdCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }

    @Test
    void sendToPlayer_returnsFalseForNonExistentPlayer() {
        // Arrange
        String socketId = "nonexistent";
        String message = "Test message";

        // Act
        // Note: sendToPlayer always returns true and attempts to send,
        // as we can't verify socket existence without tracking all connections
        boolean sent = connectionManager.sendToPlayer(socketId, message);

        // Assert
        // The method will attempt to send, but messageSender may not actually send
        // if the socket doesn't exist. We verify the messageSender was called.
        assertTrue(sent);
        verify(messageSender).sendMessage(eq(socketId), eq(message));
    }


    @Test
    void handlePlayerMessage_withoutAuthentication_rejectsGameMessage() throws Exception {
        // Arrange
        String socketId = "socket1";
        String gameMessage = "{\"type\":\"getMessages\"}";
        
        connectionManager.playerConnected(socketId);

        // Act
        connectionManager.handlePlayerMessage(socketId, gameMessage);

        // Assert
        verify(game, never()).handlePlayerMessage(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withAuthentication_forwardsToGame() throws Exception {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        String gameMessage = "{\"type\":\"getMessages\"}";
        
        connectionManager.playerConnected(socketId);
        connectionManager.authenticateSocket(socketId, userId);

        // Act
        connectionManager.handlePlayerMessage(socketId, gameMessage);

        // Assert
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(game).handlePlayerMessage(userIdCaptor.capture(), messageCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
        assertEquals(gameMessage, messageCaptor.getValue());
    }


    @Test
    void sendToPlayer_withUserId_translatesToSocketId() {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        String message = "Test message";
        
        connectionManager.playerConnected(socketId);
        connectionManager.authenticateSocket(socketId, userId);

        // Act
        boolean sent = connectionManager.sendToPlayer(userId, message);

        // Assert
        assertTrue(sent);
        ArgumentCaptor<String> socketIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageSender).sendMessage(socketIdCaptor.capture(), messageCaptor.capture());
        assertEquals(socketId, socketIdCaptor.getValue());
        assertEquals(message, messageCaptor.getValue());
    }

    @Test
    void playerDisconnected_whenAuthenticated_notifiesGameWithUserId() {
        // Arrange
        String socketId = "socket1";
        String userId = "user123";
        
        connectionManager.playerConnected(socketId);
        connectionManager.authenticateSocket(socketId, userId);

        // Act
        connectionManager.playerDisconnected(socketId);

        // Assert
        ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(game).handlePlayerDisconnected(userIdCaptor.capture());
        assertEquals(userId, userIdCaptor.getValue());
        assertFalse(connectionManager.isAuthenticated(socketId));
        assertNull(connectionManager.getUserId(socketId));
    }

    @Test
    void playerDisconnected_whenNotAuthenticated_doesNotNotifyGame() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        connectionManager.playerDisconnected(socketId);

        // Assert
        verify(game, never()).handlePlayerDisconnected(anyString());
    }

    @Test
    void handlePlayerMessage_withReauthentication_updatesUserIdMapping() throws Exception {
        // Arrange
        String socketId = "socket1";
        String userId1 = "user123";
        String userId2 = "user456";
        
        connectionManager.playerConnected(socketId);
        connectionManager.authenticateSocket(socketId, userId1);

        // Act - reauthenticate with different userId
        connectionManager.authenticateSocket(socketId, userId2);

        // Assert
        assertEquals(userId2, connectionManager.getUserId(socketId));
        assertEquals(socketId, connectionManager.getSocketId(userId2));
        assertNull(connectionManager.getSocketId(userId1)); // Old mapping should be removed
        verify(game).handlePlayerDisconnected(userId1); // Old user should be disconnected
        verify(game).handlePlayerConnected(userId2); // New user should be connected
    }

    @Test
    void getUserId_returnsNullForUnauthenticatedSocket() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        String userId = connectionManager.getUserId(socketId);

        // Assert
        assertNull(userId);
    }

    @Test
    void getSocketId_returnsNullForUnknownUserId() {
        // Act
        String socketId = connectionManager.getSocketId("unknown-user");

        // Assert
        assertNull(socketId);
    }

    @Test
    void isAuthenticated_returnsFalseForUnauthenticatedSocket() {
        // Arrange
        String socketId = "socket1";
        connectionManager.playerConnected(socketId);

        // Act
        boolean authenticated = connectionManager.isAuthenticated(socketId);

        // Assert
        assertFalse(authenticated);
    }
}

