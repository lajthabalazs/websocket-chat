package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.PlayerConnection;
import ca.lajtha.websocketchat.game.Game;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class ChatGameController implements Game {

    private final ChatGame game;
    private final PlayerConnection playerConnection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ChatGameController(ChatGame game, PlayerConnection playerConnection) {
        this.game = game;
        this.playerConnection = playerConnection;
    }

    /**
     * Deserializes a JSON message string into one of the game message commands.
     * Expected JSON formats:
     * - Get messages: {"type": "getMessages"}
     * - Send message: {"type": "sendMessage", "message": "message text"}
     * - Get players: {"type": "getPlayers"}
     *
     * @param jsonMessage the JSON string to deserialize
     * @return the deserialized ChatGameMessage command
     * @throws IllegalArgumentException if the JSON format is invalid or the type is unknown
     */
    private ChatGameMessage deserializeMessage(String jsonMessage) {
        try {
            return objectMapper.readValue(jsonMessage, ChatGameMessage.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    /**
     * Serializes a ChatGameMessage to JSON string.
     *
     * @param message the message to serialize
     * @return the JSON string representation
     * @throws IllegalArgumentException if serialization fails
     */
    private String serializeMessage(ChatGameMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize message: " + e.getMessage(), e);
        }
    }

    @Override
    public void handlePlayerMessage(String playerId, String message) {
        ChatGameMessage command = deserializeMessage(message);
        
        ChatGameMessage response = switch (command) {
            case GetMessagesCommand ignored -> {
                List<Message> messages = game.getMessages();
                yield new GetMessagesResponse(messages);
            }
            case SendMessageCommand sendCommand -> {
                game.addMessage(playerId, sendCommand.message());
                yield null; // No response needed for send message
            }
            case GetPlayersCommand ignored -> {
                List<String> players = game.getPlayers();
                yield new GetPlayersResponse(players);
            }
            default -> null;
        };
        
        if (response != null) {
            String serializedResponse = serializeMessage(response);
            playerConnection.sendToPlayer(playerId, serializedResponse);
        }
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
