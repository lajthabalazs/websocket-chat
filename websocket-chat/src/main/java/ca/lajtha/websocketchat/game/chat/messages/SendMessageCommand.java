package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("sendMessage")
public record SendMessageCommand(String message) implements ChatGameMessage {
}

