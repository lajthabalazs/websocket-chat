package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("messageReceivedNotification")
public record MessageReceivedNotification(String screenName, String message) implements ChatGameMessage {
}

