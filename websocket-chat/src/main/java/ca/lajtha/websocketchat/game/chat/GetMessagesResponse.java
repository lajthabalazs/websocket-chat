package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("getMessagesResponse")
public record GetMessagesResponse(List<VisibleMessage> messages) implements ChatGameMessage {
}


