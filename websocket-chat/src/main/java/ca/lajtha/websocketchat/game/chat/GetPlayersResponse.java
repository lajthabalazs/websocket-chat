package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("getPlayersResponse")
public record GetPlayersResponse(List<String> players) implements ChatGameMessage {
}

