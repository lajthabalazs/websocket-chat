package ca.lajtha.websocketchat.game.chat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = GetMessagesCommand.class, name = "getMessages"),
    @JsonSubTypes.Type(value = SendMessageCommand.class, name = "sendMessage"),
    @JsonSubTypes.Type(value = GetPlayersCommand.class, name = "getPlayers"),
    @JsonSubTypes.Type(value = GetMessagesResponse.class, name = "getMessagesResponse"),
    @JsonSubTypes.Type(value = GetPlayersResponse.class, name = "getPlayersResponse"),
    @JsonSubTypes.Type(value = PlayerJoinedChatNotification.class, name = "playerJoinedChatNotification"),
    @JsonSubTypes.Type(value = PlayerLeftChatNotification.class, name = "playerLeftChatNotification"),
    @JsonSubTypes.Type(value = MessageReceivedNotification.class, name = "messageReceivedNotification")
})
public interface ChatGameMessage {
}
