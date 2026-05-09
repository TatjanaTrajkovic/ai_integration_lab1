package org.example.ai_integration_tatjana.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Global felhanterare for hela applikationen.
 *
 * @RestControllerAdvice gor att den har klassen "lyssnar" pa alla
 * undantag (exceptions) som kastas fran alla controllers.
 * Istallet for att krascha eller returnera en ful stacktrace
 * till anvandaren, fanger vi felet har och returnerar ett snyggt JSON-svar.
 *
 * Utan den har klassen skulle anvandaren fa ett otydligt 500-fel.
 * Med den har klassen far de ett tydligt meddelande om vad som gick fel.
 *
 * Exempel pa svar vid fel:
 * {
 *   "status": 503,
 *   "error": "AI-tjansten ar tillfalligt otillganglig",
 *   "details": "503 Service Unavailable: ...",
 *   "timestamp": "2025-01-15T10:30:00"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Hanterar fel fran OpenRouter (5xx-svar).
     *
     * HttpServerErrorException kastas av RestClient nar OpenRouter svarar
     * med ett 5xx-fel (t.ex. 503 Service Unavailable eller 500 Internal Server Error).
     * Det har inträffar nar OpenRouter ar nere eller overbelastat - och det händer
     * aven efter att @Retryable i LlmClient har forsökt 3 ganger utan framgang.
     *
     * Vi returnerar HTTP 503 till anvandaren med ett forklarande meddelande.
     *
     * @param ex  undantaget som kastades av RestClient
     * @return    ett snyggt JSON-felmeddelande med statuskod 503
     */
    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<Map<String, Object>> handleAiServiceError(HttpServerErrorException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "error", "AI-tjansten ar tillfalligt otillganglig. Forsok igen om en stund.",
                        "details", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    /**
     * Generell fallback-hanterare for alla andra oväntade fel.
     *
     * Om ett fel inträffar som inte matchas av nagon annan @ExceptionHandler
     * hamnar det har. Det ar en sakehetsnät som gor att anvandaren aldrig
     * ser en ful Java-stacktrace i webbläsaren.
     *
     * Vi returnerar HTTP 500 med ett generellt felmeddelande.
     *
     * @param ex  vilket undantag som helst
     * @return    ett snyggt JSON-felmeddelande med statuskod 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralError(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "Ett oväntat fel inträffade",
                        "details", ex.getMessage(),
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
}