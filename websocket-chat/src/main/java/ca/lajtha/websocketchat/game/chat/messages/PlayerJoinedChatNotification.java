package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("playerJoinedChatNotification")
public record PlayerJoinedChatNotification(String screenName) implements ChatGameMessage {
}

