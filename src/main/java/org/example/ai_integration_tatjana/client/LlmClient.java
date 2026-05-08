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

/**
 * Ansvarar för all kommunikation med det externa AI-API:t (OpenRouter).
 *
 * Den här klassen är applikationens "telefon" mot omvärlden — den enda
 * platsen där vi pratar med OpenRouter. Om vi byter AI-leverantör
 * är det bara den här klassen vi behöver ändra.
 *
 * Flödet för ett anrop:
 *   1. Vi tar emot en lista av meddelanden (historik + systempromt + ny fråga)
 *   2. Vi bygger en OpenRouterRequest och skickar den via RestClient
 *   3. OpenRouter svarar med JSON som vi parsar till OpenRouterResponse
 *   4. Vi plockar ut AI:ns svartext och returnerar den
 */
@Service
public class LlmClient {

    /**
     * RestClient-instansen som skapades i AppConfig.
     * Spring injekterar den automatiskt via konstruktorn (Constructor Injection).
     * Det är det rekommenderade sättet att injekta beroenden i Spring.
     */
    private final RestClient restClient;

    /**
     * API-nyckeln läses in från application.properties via @Value.
     * I application.properties ser det ut såhär:
     *   openrouter.api.key=${OPENROUTER_API_KEY}
     *
     * ${OPENROUTER_API_KEY} betyder: läs värdet från miljövariabeln OPENROUTER_API_KEY.
     * På det sättet hårdkodar vi aldrig nyckeln i koden — den hålls säker utanför projektet.
     */
    @Value("${openrouter.api.key}")
    private String apiKey;

    /**
     * Vilken AI-modell vi vill använda.
     * Syntaxen ${openrouter.model:openai/gpt-4o-mini} betyder:
     * "läs openrouter.model från properties, och om den saknas använd 'openai/gpt-4o-mini' som standard"
     */
    @Value("${openrouter.model:openai/gpt-4o-mini}")
    private String model;

    /**
     * Konstruktorn tar emot RestClient via Dependency Injection.
     * Spring ser att vi behöver en RestClient och ger oss den @Bean vi skapade i AppConfig.
     */
    public LlmClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Skickar en lista av meddelanden till OpenRouter och returnerar AI:ns svar.
     *
     * @Retryable gör att Spring automatiskt försöker igen om anropet misslyckas.
     * Det är ett VG-krav och fungerar tack vare @EnableRetry i AppConfig.
     *
     * value          = vilka fel som ska trigga ett nytt försök (503 = tjänsten nere, 429 = rate limit)
     * maxAttempts    = försök max 3 gånger totalt (1 vanligt + 2 återförsök)
     * backoff.delay  = vänta 1 sekund innan första återförsöket
     * backoff.multiplier = fördubbla väntetiden för varje nytt försök (1s → 2s → 4s)
     *                      Det kallas exponentiell backoff.
     *
     * @param messages  hela konversationen (systempromt + historik + ny fråga)
     * @return          AI:ns svartext som en String
     */
    @Retryable(
            retryFor = { HttpServerErrorException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String callModel(List<Message> messages) {
        // Bygg request-objektet som OpenRouter förväntar sig
        var request = new OpenRouterRequest(model, messages);

        // Skicka POST-anropet och parsa svaret till OpenRouterResponse
        var response = restClient.post()
                .uri("/chat/completions")
                // Authorization-headern krävs av OpenRouter för att identifiera oss
                .header("Authorization", "Bearer " + apiKey)
                // Vi skickar JSON
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                // Jackson omvandlar JSON-svaret automatiskt till ett OpenRouterResponse-objekt
                .body(OpenRouterResponse.class);

        // Plocka ut AI:ns svartext från det första valet i listan
        // choices.get(0) = det första (och enda) svarsalternativet
        // .message().content() = själva texten
        return response.choices().get(0).message().content();
    }

    // -------------------------------------------------------------------------
    // Inre records — representerar det JSON-format som OpenRouter använder
    // De är privata eftersom de bara behövs inuti den här klassen
    // -------------------------------------------------------------------------

    /**
     * Det vi SKICKAR till OpenRouter.
     * Jackson omvandlar automatiskt detta record till JSON.
     *
     * Resultat i JSON:
     * {
     *   "model": "openai/gpt-4o-mini",
     *   "messages": [ {"role": "system", "content": "..."}, ... ]
     * }
     */
    private record OpenRouterRequest(String model, List<Message> messages) {}

    /**
     * Det vi FÅR TILLBAKA från OpenRouter.
     * Jackson parsar JSON-svaret och fyller i dessa fält automatiskt.
     *
     * OpenRouters svar ser ut såhär i JSON:
     * {
     *   "choices": [
     *     { "message": { "role": "assistant", "content": "AI:ns svar här..." } }
     *   ]
     * }
     */
    private record OpenRouterResponse(List<Choice> choices) {}

    /**
     * Ett enskilt svarsalternativ från OpenRouter.
     * Vi använder vår befintliga Message-klass (role + content) eftersom
     * formatet är exakt detsamma.
     */
    private record Choice(Message message) {}
}