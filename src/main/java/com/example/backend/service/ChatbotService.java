//package com.example.backend.service;
//
//import com.example.backend.dto.CommonResponse;
//import com.example.backend.service.util.ResponseHandler;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//
//@Service
//@Slf4j
//public class ChatbotService {
//
//    @Value("${google.ai.api-key}")
//    private String apiKey;
//
//    @Value("${google.gemini.model-name}")
//    private String modelName;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    public ResponseEntity<CommonResponse<String>> getResponse(String prompt) {
//
//        try {
//
//            // 1. Build URL
//            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
//                    + modelName
//                    + ":generateContent?key="
//                    + apiKey;
//
//            // 2. System prompt
//            String systemPrompt = """
//                    You are Red Kango AI assistant.
//
//                    Help users with:
//                    - camping
//                    - tents
//                    - outdoor equipment
//                    - hiking tips
//                    - rentals
//
//                    Keep answers friendly and short.
//                    """;
//
//            String finalPrompt = systemPrompt + "\n\nUser Question:\n" + prompt;
//
//            // 3. Build JSON safely using Jackson (IMPORTANT FIX)
//            ObjectNode root = objectMapper.createObjectNode();
//            ArrayNode contents = root.putArray("contents");
//
//            ObjectNode content = contents.addObject();
//            ArrayNode parts = content.putArray("parts");
//
//            ObjectNode part = parts.addObject();
//            part.put("text", finalPrompt);
//
//            String requestBody = objectMapper.writeValueAsString(root);
//
//            // 4. HTTP Request
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(url))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                    .build();
//
//            HttpClient client = HttpClient.newHttpClient();
//
//            HttpResponse<String> response =
//                    client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            // 5. Log raw response (VERY IMPORTANT for debugging)
//            log.info("Gemini Response: {}", response.body());
//
//            // 6. Parse response safely
//            JsonNode json = objectMapper.readTree(response.body());
//
//            // Handle API error response
//            if (json.has("error")) {
//                log.error("Gemini API error: {}", json.get("error"));
//                throw new RuntimeException("Gemini API returned error: " + json.get("error").toString());
//            }
//
//            JsonNode candidates = json.path("candidates");
//
//            if (!candidates.isArray() || candidates.isEmpty()) {
//                log.error("No candidates found. Full response: {}", response.body());
//                throw new RuntimeException("No response from Gemini API");
//            }
//
//            JsonNode firstCandidate = candidates.get(0);
//
//            JsonNode textNode = firstCandidate
//                    .path("content")
//                    .path("parts")
//                    .path(0)
//                    .path("text");
//
//            if (textNode.isMissingNode() || textNode.isNull()) {
//                log.error("Missing text field. Full response: {}", response.body());
//                throw new RuntimeException("Invalid Gemini response structure");
//            }
//
//            String aiText = textNode.asText();
//
//            // 7. Return success response
//            return ResponseHandler.generateResponse(
//                    HttpStatus.OK,
//                    "Success",
//                    "SUCCESS",
//                    aiText
//            );
//
//        } catch (Exception e) {
//
//            log.error("Chatbot error", e);
//
//            return ResponseHandler.generateErrorResponse(
//                    HttpStatus.INTERNAL_SERVER_ERROR,
//                    "Failed to get AI response",
//                    "ERROR",
//                    null
//            );
//        }
//    }
//}

package com.example.backend.service;

import com.example.backend.domain.CampingTip;
import com.example.backend.domain.Product;
import com.example.backend.dto.CommonResponse;
import com.example.backend.repository.CampingTipRepository;
import com.example.backend.repository.ProductRepository;
import com.example.backend.service.util.ResponseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.gemini.model-name}")
    private String modelName;

    private final ProductRepository productRepository;
    private final CampingTipRepository campingTipRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<CommonResponse<String>> getResponse(String prompt) {

        try {
            // 1. Fetch live data from database
            String context = buildContext(prompt);

            // 2. Build URL
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + modelName
                    + ":generateContent?key="
                    + apiKey;

            // 3. System prompt with injected database context
            String systemPrompt = """
                    You are the RedKango AI assistant — a helpful and friendly chatbot for RedKango,\s
                    a camping and outdoor equipment rental and sales platform in Sri Lanka.
                    
                    Your job is to help customers with:
                    - Questions about available products (tents, sleeping bags, camping gear, etc.)
                    - Rental pricing and availability
                    - Booking and payment process
                    - Delivery and pickup options
                    - Camping tips and guides
                    - General outdoor advice
                    
                    IMPORTANT RULES:
                    - Always use the CURRENT DATA provided below to answer product and pricing questions.
                    - If a product is listed as OUT_OF_STOCK, inform the customer clearly.
                    - For rental items show the daily rate (price per day).
                    - For sale items show the price.
                    - Keep answers concise, friendly, and helpful.
                    - If the question is not related to RedKango or camping, politely redirect.
                    - Never make up products or prices — only use data provided below.
                    - Currency is Sri Lankan Rupees (LKR).
                    
                    REDKANGO BUSINESS INFO:
                    - Location: Sri Lanka
                    - Delivery charge: LKR 1,000 for purchases
                    - Rental payment: 50% advance online, remaining balance on delivery/pickup
                    - Rental options: Home Delivery or Store Pickup
                    - Contact: support@redkango.com | +94 71 165 3766
                    
                    """ + context;

            String finalPrompt = systemPrompt + "\n\nCustomer Question: " + prompt;

            // 4. Build JSON request body
            ObjectNode root = objectMapper.createObjectNode();
            ArrayNode contents = root.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            ObjectNode part = parts.addObject();
            part.put("text", finalPrompt);

            String requestBody = objectMapper.writeValueAsString(root);

            // 5. HTTP Request to Gemini
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("Gemini Response: {}", response.body());

            // 6. Parse response
            JsonNode json = objectMapper.readTree(response.body());

            // Handle API error response
            if (json.has("error")) {
                JsonNode errorNode = json.get("error");
                int errorCode = errorNode.path("code").asInt(0);
                log.error("Gemini API error: {}", errorNode);

                if (errorCode == 503 || errorCode == 429) {
                    // Rate limit or high demand — return friendly message
                    return ResponseHandler.generateResponse(
                            HttpStatus.OK,
                            "Success",
                            "SUCCESS",
                            "I'm currently experiencing high demand. Please try again in a moment!"
                    );
                }

                throw new RuntimeException("Gemini API returned error: " + errorNode.toString());
            }


            JsonNode candidates = json.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                log.error("No candidates found. Full response: {}", response.body());
                throw new RuntimeException("No response from Gemini API");
            }

            JsonNode textNode = candidates.get(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text");

            if (textNode.isMissingNode() || textNode.isNull()) {
                throw new RuntimeException("Invalid Gemini response structure");
            }

            return ResponseHandler.generateResponse(
                    HttpStatus.OK, "Success", "SUCCESS", textNode.asText()
            );

        } catch (Exception e) {
            log.error("Chatbot error", e);
            return ResponseHandler.generateErrorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get AI response", "ERROR", null
            );
        }
    }

    /**
     * Builds a context string from live database data.
     * Always loads all products and camping tips to give Gemini accurate information.
     */
    private String buildContext(String prompt) {
        StringBuilder context = new StringBuilder();

        // ── Products ─────────────────────────────────────────────────────────
        List<Product> products = productRepository.findAll();

        List<Product> rentalProducts = products.stream()
                .filter(p -> "RENTAL".equals(p.getType().name()))
                .toList();

        List<Product> saleProducts = products.stream()
                .filter(p -> "SALE".equals(p.getType().name()))
                .toList();

        if (!rentalProducts.isEmpty()) {
            context.append("=== RENTAL PRODUCTS (available for rent) ===\n");
            for (Product p : rentalProducts) {
                context.append("- ").append(p.getProductName())
                        .append(" | Daily Rate: LKR ").append(String.format("%.0f", p.getPrice()))
                        .append(" | Available Units: ").append(p.getAvailableUnits())
                        .append("/").append(p.getTotalUnits())
                        .append(" | Status: ").append(p.getAvailableUnits() > 0 ? "Available" : "Currently Unavailable");
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    context.append(" | ").append(p.getDescription());
                }
                context.append("\n");
            }
            context.append("\n");
        }

        if (!saleProducts.isEmpty()) {
            context.append("=== SALE PRODUCTS (available for purchase) ===\n");
            for (Product p : saleProducts) {
                context.append("- ").append(p.getProductName())
                        .append(" | Price: LKR ").append(String.format("%.0f", p.getPrice()))
                        .append(" | Stock: ").append(p.getAvailableUnits())
                        .append(" units | Status: ").append(p.getAvailableUnits() > 0 ? "In Stock" : "Out of Stock");
                if (p.getDescription() != null && !p.getDescription().isBlank()) {
                    context.append(" | ").append(p.getDescription());
                }
                context.append("\n");
            }
            context.append("\n");
        }

        // ── Camping Tips ──────────────────────────────────────────────────────
        List<CampingTip> tips = campingTipRepository.findByPublishedTrueOrderByCreatedAtDesc();

        if (!tips.isEmpty()) {
            context.append("=== CAMPING TIPS & GUIDES ===\n");
            for (CampingTip tip : tips) {
                context.append("- ").append(tip.getTitle());
                if (tip.getSummary() != null && !tip.getSummary().isBlank()) {
                    context.append(": ").append(tip.getSummary());
                }
                context.append("\n");
            }
            context.append("\n");
        }

        if (context.isEmpty()) {
            context.append("No product or guide data currently available.\n");
        }

        return "=== CURRENT REDKANGO DATA (use this to answer questions) ===\n"
                + context.toString();
    }
}
