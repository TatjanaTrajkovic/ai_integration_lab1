package org.example.ai_integration_tatjana.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * Konfigurationsklass for applikationen.
 *
 * @Configuration talar om for Spring att den har klassen innehaller
 * konfiguration - Spring laser igenom den vid uppstart och registrerar
 * alla @Bean-metoder.
 *
 * @EnableRetry aktiverar Spring Retry i hela applikationen.
 * Utan den har annotationen fungerar inte @Retryable i LlmClient,
 * aven om beroendet finns i pom.xml.
 */
@Configuration
@EnableRetry
public class AppConfig {

    /**
     * Base-URL:en lases fran application.properties via @Value.
     * Standardvarde ar https://openrouter.ai (anvands i produktion).
     *
     * Varfor konfigurerbar?
     * I tester pekar vi om den har URL:en till WireMock (ett fejkat API pa localhost)
     * istallet for det riktiga OpenRouter. Pa sa satt slipper vi anvanda en riktig
     * API-nyckel eller gora riktiga natverksanrop i tester.
     *
     * Syntaxen ${openrouter.base-url:https://openrouter.ai} betyder:
     * "las openrouter.base-url fran properties, om den saknas anvand https://openrouter.ai"
     */
    @Value("${openrouter.base-url:https://openrouter.ai}")
    private String baseUrl;

    /**
     * Skapar en RestClient-instans som Spring hanterar och delar med hela appen.
     *
     * @Bean betyder: "Spring, skapa det har objektet en gang och injekta det
     * dit det behovs - vi behover inte skriva 'new RestClient()' sjalva."
     *
     * @return en konfigurerad RestClient-instans
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                // Grundadressen lases fran properties - gor det enkelt att byta miljo
                .baseUrl(baseUrl)
                .build();
    }
}