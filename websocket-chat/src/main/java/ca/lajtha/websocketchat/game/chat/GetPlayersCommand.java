package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("getPlayers")
public record GetPlayersCommand() implements ChatGameMessage {
}

