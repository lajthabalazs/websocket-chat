package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("getMessages")
public record GetMessagesCommand() implements ChatGameMessage {
}

