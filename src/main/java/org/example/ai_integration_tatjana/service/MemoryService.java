package org.example.ai_integration_tatjana.service;

import org.example.ai_integration_tatjana.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ansvarar för att lagra och hämta chatthistorik per session i minnet.
 *
 * Utan minne skulle AI:n glömma allt mellan varje meddelande:
 *   Användare: "Jag heter Anna."
 *   AI:       "Hej! Hur kan jag hjälpa dig?"
 *   Användare: "Vad heter jag?"
 *   AI:       "Det vet jag inte." ← FEL! AI:n har inget minne.
 *
 * Med minne skickar vi hela historiken vid varje anrop:
 *   [user: "Jag heter Anna.", assistant: "Hej Anna!", user: "Vad heter jag?"]
 *   AI:  "Du heter Anna!" ← RÄTT!
 *
 * Minnet lagras i en Map där:
 *   Nyckel = sessionId (t.ex. "user-123-abc")
 *   Värde  = lista av Message-objekt (hela konversationshistoriken)
 *
 * VIKTIGT: Detta är ett "in-memory" lager — allt försvinner när appen startas om.
 * För persistent lagring skulle man använda en databas, men det är utanför scope för labben.
 */
@Service
public class MemoryService {

    /**
     * Själva minnet — en Map som håller historiken för alla aktiva sessioner.
     *
     * Vi använder ConcurrentHashMap istället för vanlig HashMap eftersom
     * en webbapplikation hanterar FLERA användare samtidigt (parallella trådar).
     * ConcurrentHashMap är trådsäker — två användare kan läsa/skriva samtidigt
     * utan att krocka eller förstöra varandras data.
     *
     * HashMap är INTE trådsäker och kan ge oförutsägbara fel i en webbapp.
     */
    private final ConcurrentHashMap<String, List<Message>> memory = new ConcurrentHashMap<>();

    /**
     * Hämtar hela konversationshistoriken för en given session.
     *
     * Om sessionId inte finns ännu returneras en tom lista — aldrig null.
     * Det gör att vi slipper null-kontroller på andra ställen i koden.
     *
     * @param sessionId  unikt ID för sessionen/konversationen
     * @return           lista med alla tidigare meddelanden, eller tom lista om ny session
     */
    public List<Message> getHistory(String sessionId) {
        // getOrDefault() returnerar listan om sessionId finns,
        // annars returneras en ny tom ArrayList
        return memory.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * Lägger till ett nytt meddelande i historiken för en given session.
     *
     * computeIfAbsent() gör två saker i ett steg:
     *   1. Om sessionId INTE finns → skapa en ny tom ArrayList och lägg in den i map:en
     *   2. Returnera listan (oavsett om den var ny eller redan fanns)
     * Sedan lägger vi till det nya meddelandet i listan.
     *
     * @param sessionId  unikt ID för sessionen/konversationen
     * @param message    meddelandet som ska läggas till (role + content)
     */
    public void addMessage(String sessionId, Message message) {
        // computeIfAbsent skapar listan om den inte finns, annars används den befintliga
        memory.computeIfAbsent(sessionId, key -> new ArrayList<>()).add(message);
    }

    /**
     * Rensar hela historiken för en given session.
     *
     * Användbart om man vill starta om en konversation från början
     * utan att byta sessionId.
     *
     * @param sessionId  unikt ID för sessionen som ska rensas
     */
    public void clearHistory(String sessionId) {
        memory.remove(sessionId);
    }
}