package ca.lajtha.websocketchat.server.http;

import io.micronaut.context.annotation.Factory;
import io.micronaut.http.server.cors.CorsOriginConfiguration;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;

/**
 * CORS configuration to allow the HTTP test UI (running on a different port)
 * to call the Micronaut HTTP endpoints (e.g. /auth/websocket-token) with credentials.
 */
@Factory
public class CorsConfig {

    /**
     * Configure CORS to allow http://localhost:8081 to access the Micronaut HTTP server
     * with credentials. This is primarily for local testing where the WebSocket server
     * runs on port 8080 and the HTTP UI may be served from port 8081.
     */
    @Singleton
    public Map<String, CorsOriginConfiguration> corsConfiguration() {
        CorsOriginConfiguration originConfig = new CorsOriginConfiguration();
        originConfig.setAllowedOrigins(List.of(
                "http://localhost:8081",
                "http://localhost:8080"
        ));
        originConfig.setAllowedMethods(List.of(
                io.micronaut.http.HttpMethod.GET,
                io.micronaut.http.HttpMethod.POST,
                io.micronaut.http.HttpMethod.PUT,
                io.micronaut.http.HttpMethod.DELETE,
                io.micronaut.http.HttpMethod.OPTIONS
        ));
        originConfig.setAllowedHeaders(List.of("*"));
        originConfig.setAllowCredentials(true);

        return Map.of("default", originConfig);
    }
}


