package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("getPlayersResponse")
public record GetPlayersResponse(List<String> screenNames) implements ChatGameMessage {
}

