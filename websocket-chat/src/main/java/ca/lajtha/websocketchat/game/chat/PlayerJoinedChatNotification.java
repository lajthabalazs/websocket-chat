package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("playerJoinedChatNotification")
public record PlayerJoinedChatNotification(String playerId) implements ChatGameMessage {
}

