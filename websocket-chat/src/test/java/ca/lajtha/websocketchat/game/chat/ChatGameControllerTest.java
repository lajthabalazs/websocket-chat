package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.websocket.PlayerConnection;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatGameControllerTest {

    @Mock
    private ChatGame game;

    @Mock
    private PlayerConnection playerConnection;

    private ChatGameController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        controller = new ChatGameController(game, playerConnection);
        objectMapper = new ObjectMapper();
    }

    @Test
    void handlePlayerMessage_withGetMessagesCommand_returnsMessagesResponse() throws Exception {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"getMessages\"}";
        List<Message> expectedMessages = Arrays.asList(
            new Message("player1", "Hello"),
            new Message("player2", "World")
        );
        when(game.getMessages()).thenReturn(expectedMessages);

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).getMessages();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(playerIdCaptor.capture(), responseCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue());
        String responseJson = responseCaptor.getValue();
        assertTrue(responseJson.contains("\"type\":\"getMessagesResponse\""));
        assertTrue(responseJson.contains("\"messages\""));
    }

    @Test
    void handlePlayerMessage_withSendMessageCommand_addsMessageAndNoResponse() {
        // Arrange
        String playerId = "player1";
        String messageText = "Hello, world!";
        String jsonMessage = "{\"type\":\"sendMessage\",\"message\":\"" + messageText + "\"}";

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).addMessage(playerId, messageText);
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withGetPlayersCommand_returnsPlayersResponse() {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"getPlayers\"}";
        List<String> expectedPlayers = Arrays.asList("player1", "player2", "player3");
        when(game.getPlayers()).thenReturn(expectedPlayers);

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(playerIdCaptor.capture(), responseCaptor.capture());
        
        assertEquals(playerId, playerIdCaptor.getValue());
        String responseJson = responseCaptor.getValue();
        assertTrue(responseJson.contains("\"type\":\"getPlayersResponse\""));
        assertTrue(responseJson.contains("\"players\""));
    }

    @Test
    void handlePlayerMessage_withInvalidJson_throwsException() {
        // Arrange
        String playerId = "player1";
        String invalidJson = "not valid json";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> controller.handlePlayerMessage(playerId, invalidJson)
        );
        
        assertTrue(exception.getMessage().contains("Invalid JSON format"));
        verify(game, never()).getMessages();
        verify(game, never()).addMessage(anyString(), anyString());
        verify(game, never()).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withMissingTypeField_throwsException() {
        // Arrange
        String playerId = "player1";
        String jsonWithoutType = "{\"message\":\"test\"}";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> controller.handlePlayerMessage(playerId, jsonWithoutType)
        );
        
        assertTrue(exception.getMessage().contains("Invalid JSON format") || 
                   exception.getMessage().contains("type"));
        verify(game, never()).getMessages();
        verify(game, never()).addMessage(anyString(), anyString());
        verify(game, never()).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withUnknownType_throwsException() {
        // Arrange
        String playerId = "player1";
        String jsonWithUnknownType = "{\"type\":\"unknownCommand\"}";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> controller.handlePlayerMessage(playerId, jsonWithUnknownType)
        );
        
        assertTrue(exception.getMessage().contains("Invalid JSON format") || 
                   exception.getMessage().contains("Unknown"));
        verify(game, never()).getMessages();
        verify(game, never()).addMessage(anyString(), anyString());
        verify(game, never()).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withSendMessageCommand_handlesSpecialCharacters() throws Exception {
        // Arrange
        String playerId = "player1";
        String messageText = "Hello, \"world\"!";
        SendMessageCommand command = new SendMessageCommand(messageText);
        String jsonMessage = objectMapper.writeValueAsString(command);

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).addMessage(playerId, messageText);
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withGetMessagesCommand_handlesEmptyMessagesList() {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"getMessages\"}";
        when(game.getMessages()).thenReturn(List.of());

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).getMessages();
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(eq(playerId), responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertTrue(responseJson.contains("\"type\":\"getMessagesResponse\""));
        assertTrue(responseJson.contains("\"messages\":[]"));
    }

    @Test
    void handlePlayerMessage_withGetPlayersCommand_handlesEmptyPlayersList() {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"getPlayers\"}";
        when(game.getPlayers()).thenReturn(List.of());

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(eq(playerId), responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertTrue(responseJson.contains("\"type\":\"getPlayersResponse\""));
        assertTrue(responseJson.contains("\"players\":[]"));
    }

    @Test
    void onPlayerConnected_addsPlayerToGame() {
        // Arrange
        String playerId = "player1";

        // Act
        controller.onPlayerConnected(playerId);

        // Assert
        verify(game, times(1)).addPlayer(playerId);
    }

    @Test
    void onPlayerDisconnected_removesPlayerFromGame() {
        // Arrange
        String playerId = "player1";

        // Act
        controller.onPlayerDisconnected(playerId);

        // Assert
        verify(game, times(1)).removePlayer(playerId);
    }

    @Test
    void handlePlayerMessage_withSendMessageCommand_handlesEmptyMessage() {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"sendMessage\",\"message\":\"\"}";

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        verify(game, times(1)).addMessage(playerId, "");
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void handlePlayerMessage_withGetMessagesResponse_containsCorrectMessageData() throws Exception {
        // Arrange
        String playerId = "player1";
        String jsonMessage = "{\"type\":\"getMessages\"}";
        Message message1 = new Message("player1", "Hello");
        Message message2 = new Message("player2", "World");
        List<Message> expectedMessages = Arrays.asList(message1, message2);
        when(game.getMessages()).thenReturn(expectedMessages);

        // Act
        controller.handlePlayerMessage(playerId, jsonMessage);

        // Assert
        ArgumentCaptor<String> responseCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(eq(playerId), responseCaptor.capture());
        
        String responseJson = responseCaptor.getValue();
        assertTrue(responseJson.contains("\"sender\":\"player1\""));
        assertTrue(responseJson.contains("\"message\":\"Hello\""));
        assertTrue(responseJson.contains("\"sender\":\"player2\""));
        assertTrue(responseJson.contains("\"message\":\"World\""));
    }

    @Test
    void onPlayerJoinedChat_broadcastsNotificationToAllConnectedPlayers() {
        // Arrange
        String joinedPlayerId = "player2";
        List<String> allPlayers = Arrays.asList("player1", "player2", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player2")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onPlayerJoinedChat(joinedPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> notificationCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(3)).sendToPlayer(playerIdCaptor.capture(), notificationCaptor.capture());
        
        List<String> notifiedPlayers = playerIdCaptor.getAllValues();
        assertTrue(notifiedPlayers.contains("player1"));
        assertTrue(notifiedPlayers.contains("player2"));
        assertTrue(notifiedPlayers.contains("player3"));
        
        List<String> notifications = notificationCaptor.getAllValues();
        for (String notification : notifications) {
            assertTrue(notification.contains("\"type\":\"playerJoinedChatNotification\""));
            assertTrue(notification.contains("\"playerId\":\"player2\""));
        }
    }

    @Test
    void onPlayerJoinedChat_onlySendsToConnectedPlayers() {
        // Arrange
        String joinedPlayerId = "player2";
        List<String> allPlayers = Arrays.asList("player1", "player2", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player2")).thenReturn(false);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onPlayerJoinedChat(joinedPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(2)).sendToPlayer(playerIdCaptor.capture(), anyString());
        
        List<String> notifiedPlayers = playerIdCaptor.getAllValues();
        assertTrue(notifiedPlayers.contains("player1"));
        assertTrue(notifiedPlayers.contains("player3"));
        assertFalse(notifiedPlayers.contains("player2"));
    }

    @Test
    void onPlayerLeftChat_broadcastsNotificationToAllConnectedPlayers() {
        // Arrange
        String leftPlayerId = "player2";
        List<String> allPlayers = Arrays.asList("player1", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onPlayerLeftChat(leftPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> notificationCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(2)).sendToPlayer(playerIdCaptor.capture(), notificationCaptor.capture());
        
        List<String> notifiedPlayers = playerIdCaptor.getAllValues();
        assertTrue(notifiedPlayers.contains("player1"));
        assertTrue(notifiedPlayers.contains("player3"));
        
        List<String> notifications = notificationCaptor.getAllValues();
        for (String notification : notifications) {
            assertTrue(notification.contains("\"type\":\"playerLeftChatNotification\""));
            assertTrue(notification.contains("\"playerId\":\"player2\""));
        }
    }

    @Test
    void onPlayerLeftChat_onlySendsToConnectedPlayers() {
        // Arrange
        String leftPlayerId = "player2";
        List<String> allPlayers = Arrays.asList("player1", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(false);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onPlayerLeftChat(leftPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(1)).sendToPlayer(playerIdCaptor.capture(), anyString());
        
        assertEquals("player3", playerIdCaptor.getValue());
    }

    @Test
    void onMessageReceived_broadcastsNotificationToAllConnectedPlayers() {
        // Arrange
        String senderPlayerId = "player1";
        String messageText = "Hello, everyone!";
        List<String> allPlayers = Arrays.asList("player1", "player2", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player2")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onMessageReceived(senderPlayerId, messageText);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> notificationCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(3)).sendToPlayer(playerIdCaptor.capture(), notificationCaptor.capture());
        
        List<String> notifiedPlayers = playerIdCaptor.getAllValues();
        assertTrue(notifiedPlayers.contains("player1"));
        assertTrue(notifiedPlayers.contains("player2"));
        assertTrue(notifiedPlayers.contains("player3"));
        
        List<String> notifications = notificationCaptor.getAllValues();
        for (String notification : notifications) {
            assertTrue(notification.contains("\"type\":\"messageReceivedNotification\""));
            assertTrue(notification.contains("\"playerId\":\"player1\""));
            assertTrue(notification.contains("\"message\":\"Hello, everyone!\""));
        }
    }

    @Test
    void onMessageReceived_onlySendsToConnectedPlayers() {
        // Arrange
        String senderPlayerId = "player1";
        String messageText = "Hello!";
        List<String> allPlayers = Arrays.asList("player1", "player2", "player3");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player2")).thenReturn(false);
        when(playerConnection.isPlayerConnected("player3")).thenReturn(true);

        // Act
        controller.onMessageReceived(senderPlayerId, messageText);

        // Assert
        verify(game, times(1)).getPlayers();
        ArgumentCaptor<String> playerIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(2)).sendToPlayer(playerIdCaptor.capture(), anyString());
        
        List<String> notifiedPlayers = playerIdCaptor.getAllValues();
        assertTrue(notifiedPlayers.contains("player1"));
        assertTrue(notifiedPlayers.contains("player3"));
        assertFalse(notifiedPlayers.contains("player2"));
    }

    @Test
    void onPlayerJoinedChat_handlesEmptyPlayersList() {
        // Arrange
        String joinedPlayerId = "player1";
        when(game.getPlayers()).thenReturn(List.of());

        // Act
        controller.onPlayerJoinedChat(joinedPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void onPlayerLeftChat_handlesEmptyPlayersList() {
        // Arrange
        String leftPlayerId = "player1";
        when(game.getPlayers()).thenReturn(List.of());

        // Act
        controller.onPlayerLeftChat(leftPlayerId);

        // Assert
        verify(game, times(1)).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void onMessageReceived_handlesEmptyPlayersList() {
        // Arrange
        String senderPlayerId = "player1";
        String messageText = "Hello!";
        when(game.getPlayers()).thenReturn(List.of());

        // Act
        controller.onMessageReceived(senderPlayerId, messageText);

        // Assert
        verify(game, times(1)).getPlayers();
        verify(playerConnection, never()).sendToPlayer(anyString(), anyString());
    }

    @Test
    void onMessageReceived_handlesSpecialCharactersInMessage() {
        // Arrange
        String senderPlayerId = "player1";
        String messageText = "Hello, \"world\"!";
        List<String> allPlayers = Arrays.asList("player1", "player2");
        when(game.getPlayers()).thenReturn(allPlayers);
        when(playerConnection.isPlayerConnected("player1")).thenReturn(true);
        when(playerConnection.isPlayerConnected("player2")).thenReturn(true);

        // Act
        controller.onMessageReceived(senderPlayerId, messageText);

        // Assert
        ArgumentCaptor<String> notificationCaptor = ArgumentCaptor.forClass(String.class);
        verify(playerConnection, times(2)).sendToPlayer(anyString(), notificationCaptor.capture());
        
        List<String> notifications = notificationCaptor.getAllValues();
        for (String notification : notifications) {
            assertTrue(notification.contains("\"type\":\"messageReceivedNotification\""));
            assertTrue(notification.contains("\"playerId\":\"player1\""));
            assertTrue(notification.contains("Hello"));
        }
    }
}

