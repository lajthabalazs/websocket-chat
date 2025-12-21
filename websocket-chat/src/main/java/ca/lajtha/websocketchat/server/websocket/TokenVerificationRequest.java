package ca.lajtha.websocketchat.server.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Message sent by the client to verify a JWT token and authenticate the connection.
 */
public record TokenVerificationRequest(
    @JsonProperty("type") String type,
    @JsonProperty("token") String token
) {
    public static final String MESSAGE_TYPE = "verifyToken";
}

