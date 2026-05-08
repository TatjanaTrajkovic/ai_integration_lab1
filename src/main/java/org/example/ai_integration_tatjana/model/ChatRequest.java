package org.example.ai_integration_tatjana.model;

/**
 * Representerar det JSON-objekt som användaren skickar till vår endpoint.
 *
 * Spring Boot läser automatiskt in JSON från HTTP-anropets body och
 * omvandlar det till ett ChatRequest-objekt (kallas "deserialisering").
 * Det sköts av Jackson — ett bibliotek som ingår i Spring Boot.
 *
 * Exempel på JSON som användaren skickar:
 * {
 *   "personality": "coder",
 *   "message": "Hur skriver jag en for-loop i Java?",
 *   "sessionId": "user-123-abc"
 * }
 *
 * Notera att sessionId är valfritt — om användaren inte skickar med det
 * behandlas varje anrop som en ny konversation utan historik.
 */
public record ChatRequest(

        // Styr vilken "personlighet" AI:n ska ha.
        // Möjliga värden: "helper", "pirate", "coder"
        // Obligatoriskt fält.
        String personality,

        // Användarens faktiska fråga eller meddelande till AI:n.
        // Obligatoriskt fält.
        String message,

        // Unikt ID som kopplar ihop meddelanden till samma konversation.
        // Valfritt — om det saknas startas en ny session utan historik.
        String sessionId

) {}