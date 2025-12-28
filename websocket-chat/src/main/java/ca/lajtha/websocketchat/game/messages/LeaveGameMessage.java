package ca.lajtha.websocketchat.game.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("leaveGame")
public record LeaveGameMessage(String gameId) implements GameManagerMessage {
}






