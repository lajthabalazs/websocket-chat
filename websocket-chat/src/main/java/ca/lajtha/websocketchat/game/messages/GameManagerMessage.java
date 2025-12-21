package ca.lajtha.websocketchat.game.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JoinGameMessage.class, name = "joinGame"),
    @JsonSubTypes.Type(value = LeaveGameMessage.class, name = "leaveGame")
})
public interface GameManagerMessage {
}

