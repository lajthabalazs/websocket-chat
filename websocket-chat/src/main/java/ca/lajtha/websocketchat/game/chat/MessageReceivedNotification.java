package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("messageReceivedNotification")
public record MessageReceivedNotification(String screenName, String message) implements ChatGameMessage {
}

