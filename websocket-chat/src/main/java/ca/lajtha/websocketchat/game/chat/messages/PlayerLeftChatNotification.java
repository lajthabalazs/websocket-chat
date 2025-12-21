package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("playerLeftChatNotification")
public record PlayerLeftChatNotification(String screenName) implements ChatGameMessage {
}

