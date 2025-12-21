package ca.lajtha.websocketchat.user;

/**
 * Represents a user profile containing user information.
 */
public record UserProfile(
    String userId,
    String email,
    String displayName,
    String bio
) {
    /**
     * Creates a new UserProfile with the given information.
     * 
     * @param userId the unique user identifier
     * @param email the user's email address
     * @param displayName the user's display name
     * @param bio the user's bio/description
     */
    public UserProfile {
        // Record constructor - parameters are automatically assigned to fields
    }
}


