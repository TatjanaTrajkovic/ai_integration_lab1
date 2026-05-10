package org.example.ai_integration_tatjana.service;

import org.example.ai_integration_tatjana.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MemoryService {

    private final ConcurrentHashMap<String, List<Message>> memory = new ConcurrentHashMap<>();

    public List<Message> getHistory(String sessionId) {
        return memory.getOrDefault(sessionId, new ArrayList<>());
    }

    public void addMessage(String sessionId, Message message) {
        memory.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(message);
    }

    public void clearHistory(String sessionId) {
        memory.remove(sessionId);
    }
}