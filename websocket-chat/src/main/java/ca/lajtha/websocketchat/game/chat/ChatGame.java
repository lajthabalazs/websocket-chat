package ca.lajtha.websocketchat.game.chat;

import ca.lajtha.websocketchat.game.chat.messages.*;
import ca.lajtha.websocketchat.game.PlayerMessageSender;
import ca.lajtha.websocketchat.game.Game;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;

import java.util.List;

public class ChatGame implements Game, ChatMessageListener {

    private final ChatGameModel game;
    private final PlayerMessageSender playerConnection;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ChatGame(ChatGameModel game, PlayerMessageSender playerConnection) {
        this.game = game;
        this.playerConnection = playerConnection;
        game.addListener(this);
    }

    /**
     * Deserializes a JSON message string into one of the game message commands.
     * Expected JSON formats:
     * - Get messages: {"type": "getMessages"}
     * - Send message: {"type": "sendMessage", "message": "message text"}
     * - Get players: {"type": "getPlayers"}
     * - Set screen name: {"type": "setScreenName", "screenName": "name"}
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
                List<VisibleMessage> messages = game.getMessages();
                yield new GetMessagesResponse(messages);
            }
            case SendMessageCommand sendCommand -> {
                game.addMessage(playerId, sendCommand.message());
                yield null; // No response needed for send message
            }
            case GetPlayersCommand ignored -> {
                List<PlayerInfo> playerInfos = game.getPlayers();
                yield new GetPlayersResponse(playerInfos.stream().map(PlayerInfo::screenName).sorted().toList());
            }
            case SetScreenNameCommand setNameCommand -> {
                try {
                    game.setScreenName(playerId, setNameCommand.screenName());
                    yield null; // No response needed for set screen name
                } catch (IllegalArgumentException e) {
                    // Could return an error response here if needed
                    System.err.println("Error setting screen name for player " + playerId + ": " + e.getMessage());
                    yield null;
                }
            }
            default -> null;
        };
        
        if (response != null) {
            String serializedResponse = serializeMessage(response);
            playerConnection.sendToPlayer(playerId, serializedResponse);
        }
    }

    @Override
    public void handlePlayerConnected(String playerId) {
        game.addPlayer(playerId);
    }

    @Override
    public void handlePlayerDisconnected(String playerId) {
        game.removePlayer(playerId);
    }

    /**
     * Broadcasts a notification message to all connected players.
     *
     * @param notification the notification message to broadcast
     */
    private void broadcastToAllPlayers(ChatGameMessage notification) {
        String serializedNotification = serializeMessage(notification);
        List<PlayerInfo> players = game.getPlayers();
        
        for (PlayerInfo player : players) {
            playerConnection.sendToPlayer(player.playerId(), serializedNotification);
        }
    }

    @Override
    public void onPlayerJoinedChat(String screenName) {
        PlayerJoinedChatNotification notification = new PlayerJoinedChatNotification(screenName);
        broadcastToAllPlayers(notification);
    }

    @Override
    public void onPlayerLeftChat(String screenName) {
        PlayerLeftChatNotification notification = new PlayerLeftChatNotification(screenName);
        broadcastToAllPlayers(notification);
    }

    @Override
    public void onMessageReceived(VisibleMessage visibleMessage) {
        MessageReceivedNotification notification = new MessageReceivedNotification(visibleMessage.screenName(), visibleMessage.message());
        broadcastToAllPlayers(notification);
    }
}
