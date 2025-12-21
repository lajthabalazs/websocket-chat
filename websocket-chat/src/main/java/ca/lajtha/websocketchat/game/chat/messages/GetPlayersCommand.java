package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("getPlayers")
public record GetPlayersCommand() implements ChatGameMessage {
}

