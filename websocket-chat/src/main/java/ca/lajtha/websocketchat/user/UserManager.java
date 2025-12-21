package ca.lajtha.websocketchat.user;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.util.UUID;

/**
 * Manages user authentication, registration, and profile operations.
 * Delegates data persistence to a UserDatabase implementation.
 */
public class UserManager {
    private final UserDatabase database;
    private final Argon2 argon2;
    
    // Argon2id parameters - can be adjusted based on performance requirements
    private static final int ITERATIONS = 2;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    
    /**
     * Creates a new UserManager with the specified database.
     * 
     * @param database the UserDatabase implementation to use for data storage
     */
    public UserManager(UserDatabase database) {
        this.database = database;
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
    }
    
    /**
     * Registers a new user with the given email and password.
     * 
     * @param email the user's email address
     * @param password the user's password (will be hashed before storage)
     * @return the unique user ID if registration was successful, null if email already exists
     */
    public String register(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        String passwordHash = hashPassword(password);
        return database.createUser(email.trim(), passwordHash);
    }
    
    /**
     * Updates a user's profile.
     * 
     * @param userId the unique user identifier
     * @param userProfile the updated user profile
     * @throws IllegalArgumentException if userId is null or empty, or if userProfile is null
     */
    public void updateProfile(String userId, UserProfile userProfile) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null");
        }
        
        // Verify user exists by checking if we can get a profile or if user exists by email
        // If no profile exists yet, check if user exists by email
        if (database.getProfile(userId) == null && !database.userExists(userProfile.email())) {
            // Also verify the userId matches the email
            String userIdByEmail = database.getUserIdByEmail(userProfile.email());
            if (userIdByEmail == null || !userIdByEmail.equals(userId)) {
                throw new IllegalArgumentException("User not found: " + userId);
            }
        }
        
        database.storeProfile(userId, userProfile);
    }
    
    /**
     * Authenticates a user and returns a login response with token and user ID.
     * 
     * @param email the user's email address
     * @param password the user's password (will be hashed before verification)
     * @return a UserLoginResponse containing token and userId if authentication succeeds, null otherwise
     */
    public UserLoginResponse login(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        String storedPasswordHash = database.getPasswordHash(email.trim());
        if (storedPasswordHash == null || !verifyPassword(password, storedPasswordHash)) {
            return null; // Authentication failed
        }
        
        String userId = database.getUserIdByEmail(email.trim());
        if (userId == null) {
            return null; // User not found
        }
        
        // Generate a new token
        String token = UUID.randomUUID().toString();
        database.storeToken(userId, token);
        
        return new UserLoginResponse(token, userId);
    }
    
    /**
     * Retrieves a user's profile.
     * 
     * @param userId the unique user identifier
     * @return the user profile, or null if not found
     */
    public UserProfile getProfile(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        
        return database.getProfile(userId.trim());
    }
    
    /**
     * Validates whether a token matches the stored token for a user.
     * 
     * @param userId the unique user identifier
     * @param token the token to validate
     * @return true if the token matches the stored token for the user, false otherwise
     */
    public boolean validateToken(String userId, String token) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        String storedToken = database.getToken(userId.trim());
        return storedToken != null && storedToken.equals(token);
    }
    
    /**
     * Hashes a password using Argon2id.
     * 
     * @param password the plain text password
     * @return the hashed password string
     */
    private String hashPassword(String password) {
        return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray());
    }
    
    /**
     * Verifies a password against a stored hash using Argon2id.
     * 
     * @param password the plain text password to verify
     * @param hash the stored password hash
     * @return true if the password matches the hash, false otherwise
     */
    private boolean verifyPassword(String password, String hash) {
        try {
            return argon2.verify(hash, password.toCharArray());
        } catch (Exception e) {
            // If verification fails for any reason (invalid hash format, etc.), return false
            return false;
        }
    }
}

