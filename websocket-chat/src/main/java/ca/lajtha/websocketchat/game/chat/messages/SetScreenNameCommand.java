package ca.lajtha.websocketchat.game.chat.messages;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("setScreenName")
public record SetScreenNameCommand(String screenName) implements ChatGameMessage {
}



