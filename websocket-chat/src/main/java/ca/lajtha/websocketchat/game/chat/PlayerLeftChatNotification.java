package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("playerLeftChatNotification")
public record PlayerLeftChatNotification(String screenName) implements ChatGameMessage {
}

