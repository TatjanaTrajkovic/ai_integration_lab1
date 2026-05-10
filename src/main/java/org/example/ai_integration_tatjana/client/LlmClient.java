package org.example.ai_integration_tatjana.client;

import org.example.ai_integration_tatjana.model.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class LlmClient {

    private final RestClient restClient;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String model;

    public LlmClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callModel(List<Message> messages) {
        var request = new OpenRouterRequest(model, messages);

        var response = restClient.post()
                .uri("/api/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenRouterResponse.class);

        return response.choices().get(0).message().content();
    }

    private record OpenRouterRequest(String model, List<Message> messages) {}
    private record OpenRouterResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
}