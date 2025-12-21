package ca.lajtha.websocketchat.server.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.util.Map;

@Controller("/health")
public class HealthController {
    
    @Get
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse<Map<String, String>> health() {
        return HttpResponse.ok(Map.of("status", "ok"));
    }
}


