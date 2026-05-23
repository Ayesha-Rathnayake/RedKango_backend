package com.example.backend.service;

import com.example.backend.dto.CommonResponse;
import com.example.backend.service.util.ResponseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class ChatbotService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.gemini.model-name}")
    private String modelName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<CommonResponse<String>> getResponse(String prompt) {

        try {

            // 1. Build URL
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName
                    + ":generateContent?key="
                    + apiKey;

            // 2. System prompt
            String systemPrompt = """
                    You are Red Kango AI assistant.

                    Help users with:
                    - camping
                    - tents
                    - outdoor equipment
                    - hiking tips
                    - rentals

                    Keep answers friendly and short.
                    """;

            String finalPrompt = systemPrompt + "\n\nUser Question:\n" + prompt;

            // 3. Build JSON safely using Jackson (IMPORTANT FIX)
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");

            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");

            ObjectNode part = parts.addObject();
            part.put("text", finalPrompt);

            String requestBody = objectMapper.writeValueAsString(root);

            // 4. HTTP Request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            // 5. Log raw response (VERY IMPORTANT for debugging)
            log.info("Gemini Response: {}", response.body());

            // 6. Parse response safely
            JsonNode json = objectMapper.readTree(response.body());

            // Handle API error response
            if (json.has("error")) {
                log.error("Gemini API error: {}", json.get("error"));
                throw new RuntimeException("Gemini API returned error: " + json.get("error").toString());
            }

            JsonNode candidates = json.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                log.error("No candidates found. Full response: {}", response.body());
                throw new RuntimeException("No response from Gemini API");
            }

            JsonNode firstCandidate = candidates.get(0);

            JsonNode textNode = firstCandidate
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                log.error("Missing text field. Full response: {}", response.body());
                throw new RuntimeException("Invalid Gemini response structure");
            }

            String aiText = textNode.asText();

            // 7. Return success response
            return ResponseHandler.generateResponse(
                    HttpStatus.OK,
                    "Success",
                    "SUCCESS",
                    aiText
            );

        } catch (Exception e) {

            log.error("Chatbot error", e);

            return ResponseHandler.generateErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get AI response",
                    "ERROR",
                    null
            );
        }
    }
}