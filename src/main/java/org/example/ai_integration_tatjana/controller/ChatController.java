package org.example.ai_integration_tatjana.controller;

import org.example.ai_integration_tatjana.model.ChatRequest;
import org.example.ai_integration_tatjana.model.ChatResponse;
import org.example.ai_integration_tatjana.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        ChatResponse response = chatService.handleChat(request);
        return ResponseEntity.ok(response);
    }
}