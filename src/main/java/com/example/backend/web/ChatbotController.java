package com.example.backend.web;

import com.example.backend.dto.CommonResponse;
import com.example.backend.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping(value = "/ask", consumes = "text/plain")
    public ResponseEntity<CommonResponse<String>> ask(
            @RequestBody String prompt
    ) {
        return chatbotService.getResponse(prompt);
    }
}