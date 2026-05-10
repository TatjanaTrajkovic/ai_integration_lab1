package org.example.ai_integration_tatjana.service;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PersonalityService {

    private final Map<String, String> personalities = Map.of(
            "helper", "You are a friendly and helpful assistant. Answer clearly and concisely.",
            "pirate", "You are a pirate. Always respond in pirate speak. Use 'Arrr', 'matey', and similar pirate expressions.",
            "coder",  "You are a senior software engineer. Explain code concepts clearly with practical examples."
    );

    public String getSystemPrompt(String personality) {
        return personalities.getOrDefault(personality, personalities.get("helper"));
    }
}