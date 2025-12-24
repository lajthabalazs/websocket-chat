package ca.lajtha.websocketchat.game;

import java.util.Date;

/**
 * Information about a game for listing purposes.
 */
public class GameInfo {
    private final String gameId;
    private final String name;
    private final String creatorId;
    private final Date createdAt;
    
    public GameInfo(String gameId, String name, String creatorId, Date createdAt) {
        this.gameId = gameId;
        this.name = name;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public String getName() {
        return name;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
}





