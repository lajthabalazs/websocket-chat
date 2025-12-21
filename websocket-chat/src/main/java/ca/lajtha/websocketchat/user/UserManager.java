package ca.lajtha.websocketchat.user;

import ca.lajtha.websocketchat.PropertiesLoader;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.Inject;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import java.util.Date;
import java.util.Properties;

/**
 * Manages user authentication, registration, and profile operations.
 * Delegates data persistence to a UserDatabase implementation.
 */
public class UserManager {
    private final UserDatabase database;
    private final Argon2 argon2;
    private final Algorithm jwtAlgorithm;
    private final PropertiesLoader propertiesLoader;
    
    // Argon2id parameters - can be adjusted based on performance requirements
    private static final int ITERATIONS = 2;
    private static final int MEMORY = 65536; // 64 MB
    private static final int PARALLELISM = 1;
    
    // JWT configuration
    private static final long JWT_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours
    private static final String USER_ID_CLAIM = "userId";
    private static final String DEFAULT_JWT_SECRET = "your-secret-key-change-in-production";
    
    /**
     * Creates a new UserManager with the specified database and properties loader.
     * 
     * @param database the UserDatabase implementation to use for data storage
     * @param propertiesLoader the PropertiesLoader for loading configuration
     */
    @Inject
    public UserManager(UserDatabase database, PropertiesLoader propertiesLoader) {
        this.database = database;
        this.propertiesLoader = propertiesLoader;
        this.argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);
        String jwtSecret = loadJwtSecret();
        this.jwtAlgorithm = Algorithm.HMAC256(jwtSecret);
    }
    
    /**
     * Loads the JWT secret key from server.properties file.
     * 
     * @return the JWT secret key, or a default value if not found
     */
    private String loadJwtSecret() {
        Properties props = propertiesLoader.loadProperties();
        String secret = propertiesLoader.getProperty(props, "jwt.secret", DEFAULT_JWT_SECRET);
        if (DEFAULT_JWT_SECRET.equals(secret)) {
            System.err.println("Warning: Using default JWT secret. Please change jwt.secret in server.properties for production!");
        }
        return secret;
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
        
        // Generate a JWT token containing userId
        String token = generateJwtToken(userId);
        
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
     * Validates a JWT token and verifies it contains the specified userId.
     * 
     * @param userId the unique user identifier to verify
     * @param token the JWT token to validate
     * @return true if the token is valid and contains the matching userId, false otherwise
     */
    public boolean validateToken(String userId, String token) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        try {
            DecodedJWT decodedJWT = JWT.require(jwtAlgorithm)
                    .build()
                    .verify(token);
            
            String tokenUserId = decodedJWT.getClaim(USER_ID_CLAIM).asString();
            return userId.trim().equals(tokenUserId);
        } catch (JWTVerificationException e) {
            // Token is invalid, expired, or malformed
            return false;
        }
    }
    
    /**
     * Extracts the userId from a JWT token without validating it.
     * Use this method when you only need to read the userId from a token.
     * 
     * @param token the JWT token
     * @return the userId from the token, or null if token is invalid or doesn't contain userId
     */
    public String getUserIdFromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            return decodedJWT.getClaim(USER_ID_CLAIM).asString();
        } catch (Exception e) {
            return null;
        }
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
    
    /**
     * Generates a JWT token containing the userId claim.
     * 
     * @param userId the user identifier to include in the token
     * @return the JWT token string
     */
    private String generateJwtToken(String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + JWT_EXPIRATION_MS);
        
        return JWT.create()
                .withClaim(USER_ID_CLAIM, userId)
                .withIssuedAt(now)
                .withExpiresAt(expiration)
                .sign(jwtAlgorithm);
    }
}

