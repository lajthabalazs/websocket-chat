package ca.lajtha.websocketchat.game.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("joinGame")
public record JoinGameMessage(String gameId) implements GameManagerMessage {
}

