package ca.lajtha.websocketchat.server.http.dto;

import java.util.Date;

/**
 * DTO for game description in list responses.
 */
public class GameDescription {
    private String gameId;
    private String name;
    private String creatorId;
    private Date createdAt;
    
    public GameDescription() {
    }
    
    public GameDescription(String gameId, String name, String creatorId, Date createdAt) {
        this.gameId = gameId;
        this.name = name;
        this.creatorId = creatorId;
        this.createdAt = createdAt;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}

