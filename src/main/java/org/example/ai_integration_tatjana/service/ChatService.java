package org.example.ai_integration_tatjana.service;

import org.example.ai_integration_tatjana.client.LlmClient;
import org.example.ai_integration_tatjana.model.ChatRequest;
import org.example.ai_integration_tatjana.model.ChatResponse;
import org.example.ai_integration_tatjana.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Huvudtjansten som binder ihop alla delar av applikationen.
 *
 * ChatService ar "dirigenten" - den sjalv gor ingenting avancerat,
 * men den koordinerar alla andra klasser i ratt ordning:
 *
 *   1. PersonalityService  -> hamta ratt systempromt
 *   2. MemoryService       -> hamta tidigare konversationshistorik
 *   3. Bygg ihop meddelandelistan som AI:n ska fa
 *   4. LlmClient           -> skicka till OpenRouter, fa svar
 *   5. MemoryService       -> spara det nya meddelandet och svaret
 *   6. Returnera svaret till controllern
 *
 * Tack vare denna uppdelning ar varje klass liten och fokuserad.
 * Om vi byter AI-leverantor andrar vi bara LlmClient.
 * Om vi byter minnesstrategi andrar vi bara MemoryService. Osv.
 */
@Service
public class ChatService {

    /**
     * Alla beroenden injekteras via konstruktorn (Constructor Injection).
     *
     * Det har ar det rekommenderade sattet i Spring - istallet for @Autowired
     * pa falten. Fordelarna ar att:
     *   - Klassen ar latt att testa (vi kan skicka in mock-objekt)
     *   - Det ar tydligt vad klassen beror pa
     *   - Spring garanterar att inget ar null nar objektet skapas
     */
    private final PersonalityService personalityService;
    private final MemoryService memoryService;
    private final LlmClient llmClient;

    public ChatService(PersonalityService personalityService,
                       MemoryService memoryService,
                       LlmClient llmClient) {
        this.personalityService = personalityService;
        this.memoryService = memoryService;
        this.llmClient = llmClient;
    }

    /**
     * Hanterar ett inkommande chattmeddelande fran slutanvandaren.
     *
     * Det har ar det centrala metoden i hela applikationen.
     * All logik for ett anrop floder genom den har metoden.
     *
     * @param request  innehaller personality, message och sessionId fran anvandaren
     * @return         AI:ns svar tillsammans med sessionId
     */
    public ChatResponse handleChat(ChatRequest request) {

        // STEG 1: Hantera sessionId
        // Om anvandaren inte skickat med ett sessionId genererar vi ett nytt unikt ID.
        // UUID.randomUUID() skapar ett globalt unikt ID, t.ex. "a3f2c1d4-..."
        // Det gar att varje ny konversation far sin egen isolerade historik.
        String sessionId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : UUID.randomUUID().toString();

        // STEG 2: Hamta ratt systempromt baserat pa vald personlighet
        // T.ex. "pirate" -> "You are a pirate. Always respond in pirate speak."
        String systemPrompt = personalityService.getSystemPrompt(request.personality());

        // STEG 3: Hamta konversationshistoriken for den har sessionen
        // Om det ar forsta meddelandet returneras en tom lista
        List<Message> history = memoryService.getHistory(sessionId);

        // STEG 4: Bygg ihop den fullstandiga meddelandelistan som skickas till AI:n
        // Ordningen ar VIKTIG - AI:n laser dem uppifraan och ner:
        //   [0] system    = instruktiionen (personligheten)
        //   [1..n] user/assistant = hela historiken
        //   [sist] user   = anvandararens nya fraga
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));  // Personligheten forst
        messages.addAll(history);                           // Sedan hela historiken
        messages.add(new Message("user", request.message())); // Sedan den nya fragan

        // STEG 5: Skicka till OpenRouter och ta emot AI:ns svar
        // LlmClient hanterar sjalva HTTP-anropet och retry-logiken
        String reply = llmClient.callModel(messages);

        // STEG 6: Spara det nya meddelandet och AI:ns svar i minnet
        // Vi sparar BARA det nya - historiken som kom fran getHistory() ar redan sparad
        // Nasta gang anvandaren skickar ett meddelande kommer detta ingaa i historiken
        memoryService.addMessage(sessionId, new Message("user", request.message()));
        memoryService.addMessage(sessionId, new Message("assistant", reply));

        // STEG 7: Returnera svaret till controllern
        // Vi skickar med sessionId sa att anvandaren vet vilket ID de ska
        // anvanda i nasta anrop for att fortsatta samma konversation
        return new ChatResponse(reply, sessionId);
    }
}
