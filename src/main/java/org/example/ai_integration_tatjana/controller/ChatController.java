package org.example.ai_integration_tatjana.controller;

import org.example.ai_integration_tatjana.model.ChatRequest;
import org.example.ai_integration_tatjana.model.ChatResponse;
import org.example.ai_integration_tatjana.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-controllern som exponerar applikationens endpoint mot omvarlden.
 *
 * Det har ar det yttersta lagret i applikationen - det enda som
 * anvandaren/klienten pratar direkt med.
 *
 * Ansvaret ar litet och tydligt:
 *   1. Ta emot HTTP-anropet
 *   2. Skicka vidare till ChatService
 *   3. Returnera svaret
 *
 * Ingen logik ska ligga har - allt det skoter ChatService om.
 * En tunn controller ar ett tecken pa bra arkitektur.
 *
 * @RestController  = kombinerar @Controller + @ResponseBody.
 *                   Betyder att metoder returnerar data (JSON) direkt,
 *                   inte en HTML-vy som i traditionella webbappar.
 *
 * @RequestMapping  = alla endpoints i den har klassen borjar med /api/v1
 */
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    /**
     * ChatService injekteras via konstruktorn.
     * Spring hittar automatiskt den @Service-annoterade klassen och ger oss den.
     */
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Tar emot ett chattmeddelande och returnerar AI:ns svar.
     *
     * Endpoint: POST /api/v1/chat
     *
     * @PostMapping  = den har metoden svarar pa HTTP POST-anrop till /chat
     *
     * @RequestBody  = Spring laser JSON-kroppen fran HTTP-anropet och
     *                 omvandlar den automatiskt till ett ChatRequest-objekt
     *                 (Jackson skoter omvandlingen)
     *
     * ResponseEntity<ChatResponse> = lat oss skicka tillbaka bade ett svar
     *                                och en HTTP-statuskod (t.ex. 200 OK)
     *
     * Exempel pa anrop i Insomnia:
     *   POST http://localhost:8080/api/v1/chat
     *   Body:
     *   {
     *     "personality": "pirate",
     *     "message": "Vad ar Java?",
     *     "sessionId": "test-session-1"
     *   }
     *
     * @param request  anvandararens fraga med personality, message och sessionId
     * @return         AI:ns svar med HTTP 200 OK
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        // Skicka vidare till ChatService som hanterar all logik
        ChatResponse response = chatService.handleChat(request);

        // ResponseEntity.ok() = skicka svaret med HTTP-statuskod 200 OK
        return ResponseEntity.ok(response);
    }
}