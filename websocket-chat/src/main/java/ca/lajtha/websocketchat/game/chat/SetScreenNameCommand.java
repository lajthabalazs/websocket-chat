package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("setScreenName")
public record SetScreenNameCommand(String screenName) implements ChatGameMessage {
}

