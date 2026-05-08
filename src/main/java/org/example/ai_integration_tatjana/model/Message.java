package org.example.ai_integration_tatjana.model;

/**
 * Representerar ett enskilt meddelande i en konversation.
 *
 * OpenRouter-API:t (och alla OpenAI-kompatibla API:er) förväntar sig
 * att historiken skickas som en lista av Message-objekt med två fält:
 *   - role:    vem som skickade meddelandet ("system", "user" eller "assistant")
 *   - content: själva texten
 *
 * Det är ett Java Record — en modern, kompakt datastruktur som automatiskt
 * genererar konstruktor, getters, equals(), hashCode() och toString().
 * Vi behöver alltså inte skriva någon av dessa metoder själva.
 *
 * Exempel på hur en konversation ser ut som lista av Message:
 *   [ Message("system",    "Du är en pirat. Tala som en."),
 *     Message("user",      "Vad är Java?"),
 *     Message("assistant", "Arrr, Java är ett programspråk, kompis!") ]
 */
public record Message(

        // Vem som skickade meddelandet.
        // Möjliga värden: "system" (instruktion till AI:n), "user" (slutanvändaren), "assistant" (AI:ns svar)
        String role,

        // Själva texten i meddelandet
        String content

) {}