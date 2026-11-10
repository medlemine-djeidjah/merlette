package com.merlette.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.json.JsonMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=${TEST_DATABASE_URL:jdbc:postgresql://localhost:55432/merlette_test}",
    "spring.datasource.username=${TEST_DATABASE_USERNAME:merlette}",
    "spring.datasource.password=${TEST_DATABASE_PASSWORD:merlette_test}",
    "merlette.upload-directory=target/test-uploads"
})
class MerletteBackendApplicationTests {
    @Value("${local.server.port}") int port;
    final HttpClient http = HttpClient.newHttpClient();
    final JsonMapper json = new JsonMapper();
    HttpResponse<String> request(String method, String path, String body, String token, String contentType) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api" + path));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (contentType != null) builder.header("Content-Type", contentType);
        return http.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.ISO_8859_1)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void registrationLoginAndPrivateEndpoints() throws Exception {
        String name = "test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String input = "{\"username\":\"" + name + "\",\"email\":\"" + name + "@example.org\",\"password\":\"Secret123!\"}";
        var registered = request("POST", "/auth/register", input, null, "application/json");
        assertEquals(201, registered.statusCode(), registered.body());
        var session = json.readTree(registered.body());
        String token = session.get("token").asText();
        assertEquals("USER", session.get("user").get("role").asText());
        assertFalse(registered.body().contains("passwordHash"));
        assertEquals(409, request("POST", "/auth/register", input, null, "application/json").statusCode());
        assertEquals(400, request("POST", "/auth/register", "{\"username\":\"x\",\"email\":\"bad\",\"password\":\"short\"}", null, "application/json").statusCode());
        var loggedIn = request("POST", "/auth/login", input, null, "application/json");
        assertEquals(200, loggedIn.statusCode(), loggedIn.body());
        assertEquals(401, request("POST", "/auth/login", input.replace("Secret123!", "incorrect"), null, "application/json").statusCode());
        assertEquals(401, request("GET", "/auth/me", null, null, null).statusCode());
        assertEquals(401, request("GET", "/auth/me", null, "tampered-token", null).statusCode());
        var me = request("GET", "/auth/me", null, token, null);
        assertEquals(200, me.statusCode());
        assertEquals(name, json.readTree(me.body()).get("username").asText());

        var categoryResponse = request("GET", "/categories", null, null, null);
        assertEquals(200, categoryResponse.statusCode());
        var category = json.readTree(categoryResponse.body()).get(0);
        String boundary = "MerletteTestBoundary";
        byte[] image = Files.readAllBytes(Path.of("../postman/fixtures/bird.jpg"));
        String multipart = part(boundary, "categoryId", category.get("id").asText())
            + part(boundary, "observedAt", LocalDate.now().toString())
            + part(boundary, "title", "Observation test")
            + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"photos\"; filename=\"bird.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n"
            + new String(image, StandardCharsets.ISO_8859_1) + "\r\n--" + boundary + "--\r\n";
        String type = "multipart/form-data; boundary=" + boundary;
        assertEquals(401, request("POST", "/observations", multipart, null, type).statusCode());
        var created = request("POST", "/observations", multipart, token, type);
        assertEquals(201, created.statusCode(), created.body());
        var observation = json.readTree(created.body());
        assertEquals("NEEDS_ID", observation.get("status").asText());
        assertEquals(1, observation.get("photos").size());
        assertEquals(session.get("user").get("id").asInt(), observation.get("author").get("id").asInt());
        String id = observation.get("id").asText();
        assertEquals(200, request("GET", "/observations/" + id, null, null, null).statusCode());
        var photo = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + observation.get("coverUrl").asText())).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, photo.statusCode());
        assertArrayEquals(image, photo.body());
        var feed = request("GET", "/observations?category=" + category.get("slug").asText() + "&userId=" + session.get("user").get("id").asText(), null, null, null);
        assertEquals(200, feed.statusCode(), feed.body());
        assertEquals(1, json.readTree(feed.body()).get("pagination").get("total").asInt());
        assertEquals(200, request("GET", "/users/me/observations", null, token, null).statusCode());
        assertEquals(400, request("GET", "/observations?page=0", null, null, null).statusCode());
        assertEquals(404, request("GET", "/observations/2147483647", null, null, null).statusCode());
        assertEquals(400, request("POST", "/observations", multipart.replace(LocalDate.now().toString(), LocalDate.now().plusDays(1).toString()), token, type).statusCode());
        assertEquals(400, request("POST", "/observations", multipart.replace(new String(image, StandardCharsets.ISO_8859_1), "not an image"), token, type).statusCode());
        assertEquals(200, request("GET", "/health", null, null, null).statusCode());
    }
    static String part(String boundary, String name, String value) {
        return "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n" + value + "\r\n";
    }
    @Test void browserCanRegisterFromFrontendOrigin() throws Exception {
        URI endpoint = URI.create("http://localhost:" + port + "/api/auth/register");
        var preflight = http.send(HttpRequest.newBuilder(endpoint)
            .header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, preflight.statusCode());
        assertEquals("http://localhost:5173", preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        var invalidRegistration = http.send(HttpRequest.newBuilder(endpoint)
            .header("Origin", "http://localhost:5173").header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(400, invalidRegistration.statusCode());
        assertEquals("http://localhost:5173", invalidRegistration.headers().firstValue("Access-Control-Allow-Origin").orElse(""));
        var rejectedOrigin = http.send(HttpRequest.newBuilder(endpoint)
            .header("Origin", "https://untrusted.example").header("Access-Control-Request-Method", "POST")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, rejectedOrigin.statusCode());
    }
}
