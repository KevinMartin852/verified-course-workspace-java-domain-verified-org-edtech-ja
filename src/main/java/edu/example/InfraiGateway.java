package edu.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InfraiGateway {
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json;
    private final String baseUrl;
    private final String key;

    public InfraiGateway(ObjectMapper json, @Value("${infrai.base-url}") String baseUrl) {
        this.json = json;
        this.baseUrl = baseUrl;
        this.key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
    }

    public JsonNode call(String method, String path, Map<String, ?> fields) {
        try {
            String query = method.equals("GET") ? "?" + fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
                .reduce((a, b) -> a + "&" + b).orElse("") : "";
            HttpRequest.BodyPublisher body = method.equals("GET") ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(fields));
            for (int attempt = 0; attempt < 4; attempt++) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path + query))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20)).method(method, body).build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode envelope = json.readTree(response.body());
                if (response.statusCode() == 429 && attempt < 3) {
                    long delay = response.headers().firstValue("Retry-After").map(v -> {
                        try { return Long.parseLong(v) * 1000; } catch (NumberFormatException ex) { return 0L; }
                    }).orElse(0L);
                    Thread.sleep(Math.max(delay, 250L << attempt));
                    continue;
                }
                if (!envelope.path("ok").asBoolean(false)) {
                    JsonNode error = envelope.path("error");
                    throw new ApiError(response.statusCode(), error.path("code").asText("API_ERROR"), error.toString());
                }
                if (response.statusCode() >= 500) throw new ApiError(502, "UPSTREAM_ERROR", "Upstream request failed");
                return envelope.path("data");
            }
            throw new IllegalStateException("Retry limit reached");
        } catch (ApiError e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Request interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("Request could not complete", e);
        }
    }

    public static class ApiError extends RuntimeException {
        public final int status;
        public final String code;
        public ApiError(int status, String code, String detail) {
            super(detail); this.status = status; this.code = code;
        }
    }
}
