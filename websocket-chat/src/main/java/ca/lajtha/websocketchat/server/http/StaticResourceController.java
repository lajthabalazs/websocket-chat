package ca.lajtha.websocketchat.server.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Produces;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Controller
public class StaticResourceController {
    
    @Get("/css/{filename}")
    @Produces("text/css")
    public HttpResponse<String> css(String filename) {
        String content = loadResourceFromPath("css/" + filename);
        if (content == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(content).contentType("text/css");
    }
    
    @Get("/js/{filename}")
    @Produces("application/javascript")
    public HttpResponse<String> js(String filename) {
        String content = loadResourceFromPath("js/" + filename);
        if (content == null) {
            return HttpResponse.notFound();
        }
        return HttpResponse.ok(content).contentType("application/javascript");
    }
    
    private String loadResourceFromPath(String path) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("Error loading resource " + path + ": " + e.getMessage());
            return null;
        }
    }
}

