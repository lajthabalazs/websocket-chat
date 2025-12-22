package ca.lajtha.websocketchat.server.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
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
public class IndexController {
    
    @Get("/")
    @Produces(MediaType.TEXT_HTML)
    public HttpResponse<String> index() {
        String content = loadHtmlFromResources("index.html");
        return HttpResponse.ok(content).contentType(MediaType.TEXT_HTML);
    }
    
    private String loadHtmlFromResources(String filename) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (input == null) {
                System.err.println("Warning: " + filename + " not found in resources, using fallback HTML");
                return "<html><body><h1>HTTP Server</h1><p>Server is running!</p></body></html>";
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            System.err.println("Error loading " + filename + " from resources: " + e.getMessage());
            return "<html><body><h1>HTTP Server</h1><p>Server is running!</p><p>Error loading HTML file.</p></body></html>";
        }
    }
}



