package dev.johnrich;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AITranslationService {
    private final HttpClient httpClient;
    private final Gson gson;
    private final TranslatorConfig config;

    public AITranslationService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.config = TranslatorConfig.load();
    }

    public CompletableFuture<String> translate(String language, String message) {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
                config.geminiModelId + ":generateContent?key=" + config.geminiApiKey;

        JsonObject requestBody = buildRequestBody(language, message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseResponse);
    }
    public CompletableFuture<String> translateChat(String language, String message) {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" +
                config.geminiModelId + ":generateContent?key=" + config.geminiApiKey;

        JsonObject requestBody = buildChatTranslatorRequestBody(language, message);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(this::parseResponse);
    }
    private JsonObject buildChatTranslatorRequestBody(String language, String message) {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", "Translate this to \"" + language + "\" and make sure to keep it concise (remove the user's prefixes and suffixes before the message): " + message);

        parts.add(textPart);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody;
    }

    private JsonObject buildRequestBody(String language, String message) {
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();

        JsonArray parts = new JsonArray();
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", "Translate this to \"" + language + "\" and make sure to keep it concise: " + message);
        parts.add(textPart);

        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody;
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);

            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && !parts.isEmpty()) {
                    return parts.get(0).getAsJsonObject().get("text").getAsString();
                }
            }

            return "Translation failed: Unexpected response format";
        } catch (Exception e) {
            e.printStackTrace();
            return "Translation failed: " + e.getMessage();
        }
    }
}
