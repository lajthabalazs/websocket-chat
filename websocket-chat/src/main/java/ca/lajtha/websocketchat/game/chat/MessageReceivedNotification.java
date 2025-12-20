package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("messageReceivedNotification")
public record MessageReceivedNotification(String playerId, String message) implements ChatGameMessage {
}

