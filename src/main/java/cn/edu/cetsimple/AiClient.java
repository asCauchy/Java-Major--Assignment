package cn.edu.cetsimple;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AiClient {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public String complete(AiConfig config, String systemPrompt, String userPrompt) throws Exception {
        if (!config.isReady()) throw new IllegalStateException("请先配置接口地址和模型名称。");
        ObjectNode body = config.protocol() == AiConfig.Protocol.RESPONSES
                ? responsesBody(config, systemPrompt, userPrompt)
                : chatCompletionsBody(config, systemPrompt, userPrompt);

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(config.endpoint().trim()))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        if (config.apiKey() != null && !config.apiKey().isBlank()
                && config.authHeader() != null && !config.authHeader().isBlank()) {
            String prefix = config.authPrefix() == null ? "" : config.authPrefix().trim();
            String value = prefix.isBlank() ? config.apiKey().trim() : prefix + " " + config.apiKey().trim();
            request.header(config.authHeader().trim(), value);
        }

        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = response.body() == null ? "" : response.body();
            if (error.length() > 600) error = error.substring(0, 600) + "...";
            throw new IllegalStateException("接口返回 HTTP " + response.statusCode() + "：" + error);
        }
        JsonNode root = JSON.readTree(response.body());
        String text = config.protocol() == AiConfig.Protocol.RESPONSES
                ? extractResponsesText(root)
                : extractChatText(root);
        if (text == null || text.isBlank()) throw new IllegalStateException("模型返回了空内容。");
        return text.trim();
    }

    private ObjectNode chatCompletionsBody(AiConfig config, String systemPrompt, String userPrompt) {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", config.model().trim());
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", systemPrompt);
        messages.addObject().put("role", "user").put("content", userPrompt);
        return body;
    }

    private ObjectNode responsesBody(AiConfig config, String systemPrompt, String userPrompt) {
        ObjectNode body = JSON.createObjectNode();
        body.put("model", config.model().trim());
        body.put("instructions", systemPrompt);
        body.put("input", userPrompt);
        body.put("store", false);
        return body;
    }

    private String extractChatText(JsonNode root) {
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) return content.asText();
        if (content.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : content) {
                if (item.path("text").isTextual()) text.append(item.path("text").asText());
            }
            return text.toString();
        }
        return null;
    }

    private String extractResponsesText(JsonNode root) {
        if (root.path("output_text").isTextual()) return root.path("output_text").asText();
        StringBuilder text = new StringBuilder();
        for (JsonNode output : root.path("output")) {
            for (JsonNode content : output.path("content")) {
                if (content.path("text").isTextual()) text.append(content.path("text").asText());
            }
        }
        return text.toString();
    }
}
