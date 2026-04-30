package com.dsi.studyhub.controllers;

import com.dsi.studyhub.services.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String response = aiService.chat(request.message(), request.systemPrompt() != null ? request.systemPrompt() : "");
        return ResponseEntity.ok(new ChatResponse(response));
    }

    public record ChatRequest(String message, String systemPrompt) {}
    public record ChatResponse(String response) {}
}
