package ca.lajtha.websocketchat.user;

/**
 * Represents the response from a login operation, containing authentication token and user ID.
 */
public record UserToken(
    String token,
    String userId
) {
    /**
     * Creates a new UserLoginResponse with the given token and user ID.
     * 
     * @param token the authentication token
     * @param userId the unique user identifier
     */
    public UserToken {
        // Record constructor - parameters are automatically assigned to fields
    }
}


