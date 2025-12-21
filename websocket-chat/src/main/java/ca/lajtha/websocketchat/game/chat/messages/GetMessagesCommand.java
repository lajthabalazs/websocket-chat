package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("getMessages")
public record GetMessagesCommand() implements ChatGameMessage {
}

