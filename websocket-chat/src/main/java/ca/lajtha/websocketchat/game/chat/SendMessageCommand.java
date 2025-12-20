package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("sendMessage")
public record SendMessageCommand(String message) implements ChatGameMessage {
}

