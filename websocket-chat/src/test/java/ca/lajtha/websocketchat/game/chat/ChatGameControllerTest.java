package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.PlayerConnection;
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
}

