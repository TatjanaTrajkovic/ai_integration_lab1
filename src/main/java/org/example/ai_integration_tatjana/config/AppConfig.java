package org.example.ai_integration_tatjana.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * Konfigurationsklass för applikationen.
 *
 * @Configuration talar om för Spring att den här klassen innehåller
 * konfiguration — Spring läser igenom den vid uppstart och registrerar
 * alla @Bean-metoder.
 *
 * @EnableRetry aktiverar Spring Retry i hela applikationen.
 * Utan den här annotationen fungerar inte @Retryable i LlmClient,
 * även om beroendet finns i pom.xml.
 */
@Configuration
@EnableRetry
public class AppConfig {

    /**
     * Skapar en RestClient-instans som Spring hanterar och delar med hela appen.
     *
     * @Bean betyder: "Spring, skapa det här objektet en gång och injekta det
     * dit det behövs — vi behöver inte skriva 'new RestClient()' själva."
     *
     * baseUrl sätter grundadressen för alla anrop. När vi sedan gör
     * .uri("/chat/completions") i LlmClient blir den fullständiga adressen:
     * https://openrouter.ai/api/v1/chat/completions
     *
     * @return en konfigurerad RestClient-instans
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                // Grundadressen till OpenRouter API:t.
                // Alla anrop från LlmClient utgår från denna adress.
                .baseUrl("https://openrouter.ai/api/v1")
                .build();
    }
}