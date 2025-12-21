package ca.lajtha.websocketchat.game.chat.messages;

public record PlayerInfo(String playerId, String screenName) implements Comparable<PlayerInfo>{
    @Override
    public int compareTo(PlayerInfo o) {
        if (this.screenName == null) {
            return this.playerId.compareTo(o.screenName);
        } else {
            return this.screenName.compareTo(o.screenName);
        }
    }
}



