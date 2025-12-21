package ca.lajtha.websocketchat.user;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory implementation of UserDatabase.
 * This implementation stores all data in memory and will be lost when the application restarts.
 */
public class InMemoryUserDatabase implements UserDatabase {
    private final Map<String, UserData> usersByEmail;
    private final Map<String, UserData> usersById;
    
    public InMemoryUserDatabase() {
        this.usersByEmail = new HashMap<>();
        this.usersById = new HashMap<>();
    }
    
    @Override
    public String createUser(String email, String passwordHash) {
        if (usersByEmail.containsKey(email)) {
            return null; // User already exists
        }
        
        String userId = UUID.randomUUID().toString();
        UserData userData = new UserData(userId, email, passwordHash);
        
        usersByEmail.put(email, userData);
        usersById.put(userId, userData);
        
        return userId;
    }
    
    @Override
    public String getPasswordHash(String email) {
        UserData userData = usersByEmail.get(email);
        return userData != null ? userData.passwordHash() : null;
    }
    
    @Override
    public String getUserIdByEmail(String email) {
        UserData userData = usersByEmail.get(email);
        return userData != null ? userData.userId() : null;
    }
    
    @Override
    public void storeProfile(String userId, UserProfile profile) {
        UserData userData = usersById.get(userId);
        if (userData != null) {
            userData.setProfile(profile);
        }
    }
    
    @Override
    public UserProfile getProfile(String userId) {
        UserData userData = usersById.get(userId);
        return userData != null ? userData.profile() : null;
    }
    
    @Override
    public boolean userExists(String email) {
        return usersByEmail.containsKey(email);
    }
    
    /**
     * Internal data structure to hold user information.
     */
    @SuppressWarnings("unused")
    private static class UserData {
        private final String userId;
        @SuppressWarnings("unused")
        private final String email;
        private final String passwordHash;
        private UserProfile profile;
        
        public UserData(String userId, String email, String passwordHash) {
            this.userId = userId;
            this.email = email;
            this.passwordHash = passwordHash;
            this.profile = null;
        }
        
        public String userId() {
            return userId;
        }
        
        public String passwordHash() {
            return passwordHash;
        }
        
        public UserProfile profile() {
            return profile;
        }
        
        public void setProfile(UserProfile profile) {
            this.profile = profile;
        }
    }
}

