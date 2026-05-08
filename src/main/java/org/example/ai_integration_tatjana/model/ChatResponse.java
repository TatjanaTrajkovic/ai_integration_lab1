package org.example.ai_integration_tatjana.model;

/**
 * Representerar det JSON-objekt som vi skickar tillbaka till användaren.
 *
 * Spring Boot omvandlar automatiskt detta objekt till JSON när vi
 * returnerar det från vår controller (kallas "serialisering").
 *
 * Exempel på JSON som vi svarar med:
 * {
 *   "reply": "En for-loop i Java skriver du såhär: for (int i = 0; i < 10; i++) { ... }",
 *   "sessionId": "user-123-abc"
 * }
 *
 * Vi skickar tillbaka sessionId så att användaren vet vilket ID
 * de ska använda för att fortsätta konversationen i nästa anrop.
 */
public record ChatResponse(

        // AI:ns svar på användarens fråga
        String reply,

        // Sessionens ID — samma som kom in i requesten, eller ett nytt om inget angavs.
        // Användaren sparar detta och skickar med det i nästa anrop för att fortsätta konversationen.
        String sessionId

) {}