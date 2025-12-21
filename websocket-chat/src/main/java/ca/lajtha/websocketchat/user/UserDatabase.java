package ca.lajtha.websocketchat.user;

/**
 * Interface for user data storage operations.
 * Implementations can use in-memory storage, databases, or other persistence mechanisms.
 */
public interface UserDatabase {
    /**
     * Creates a new user with the given email and password hash.
     * 
     * @param email the user's email address
     * @param passwordHash the hashed password
     * @return the unique user ID if successful, null if email already exists
     */
    String createUser(String email, String passwordHash);
    
    /**
     * Retrieves the password hash for a user by email.
     * 
     * @param email the user's email address
     * @return the password hash, or null if user not found
     */
    String getPasswordHash(String email);
    
    /**
     * Gets the user ID associated with an email.
     * 
     * @param email the user's email address
     * @return the user ID, or null if user not found
     */
    String getUserIdByEmail(String email);
    
    /**
     * Stores a user profile.
     * 
     * @param userId the user's unique identifier
     * @param profile the user profile to store
     */
    void storeProfile(String userId, UserProfile profile);
    
    /**
     * Retrieves a user profile.
     * 
     * @param userId the user's unique identifier
     * @return the user profile, or null if not found
     */
    UserProfile getProfile(String userId);
    
    /**
     * Checks if a user exists by email.
     * 
     * @param email the user's email address
     * @return true if user exists, false otherwise
     */
    boolean userExists(String email);
}

