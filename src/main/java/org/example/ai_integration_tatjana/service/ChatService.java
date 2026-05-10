package org.example.ai_integration_tatjana.service;

import org.example.ai_integration_tatjana.client.LlmClient;
import org.example.ai_integration_tatjana.model.ChatRequest;
import org.example.ai_integration_tatjana.model.ChatResponse;
import org.example.ai_integration_tatjana.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {

    private final PersonalityService personalityService;
    private final MemoryService memoryService;
    private final LlmClient llmClient;

    public ChatService(PersonalityService personalityService,
                       MemoryService memoryService,
                       LlmClient llmClient) {
        this.personalityService = personalityService;
        this.memoryService = memoryService;
        this.llmClient = llmClient;
    }

    public ChatResponse handleChat(ChatRequest request) {
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : UUID.randomUUID().toString();

        String systemPrompt = personalityService.getSystemPrompt(request.personality());
        List<Message> history = memoryService.getHistory(sessionId);

        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.addAll(history);
        messages.add(new Message("user", request.message()));

        String reply = llmClient.callModel(messages);

        memoryService.addMessage(sessionId, new Message("user", request.message()));
        memoryService.addMessage(sessionId, new Message("assistant", reply));

        return new ChatResponse(reply, sessionId);
    }
}