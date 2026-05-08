package org.example.ai_integration_tatjana.service;

import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Ansvarar för att mappa ett personlighetsnamn till en System Prompt.
 *
 * En System Prompt är en instruktion vi skickar till AI:n INNAN användarens
 * fråga. Den talar om för AI:n hur den ska bete sig och svara.
 * Den syns aldrig för slutanvändaren — det är vår hemliga konfiguration.
 *
 * Exempel på hur det fungerar i praktiken:
 *   Användaren skickar:  personality = "pirate"
 *   Vi hämtar:           "You are a pirate. Always respond in pirate speak."
 *   AI:n får:            [system: "You are a pirate..."] + [user: "Vad är Java?"]
 *   AI:n svarar:         "Arrr, Java är ett programspråk, kompis!"
 *
 * @Service talar om för Spring att denna klass är en tjänst som ska
 * hanteras av Spring's Dependency Injection — Spring skapar en instans
 * automatiskt och vi kan "injekta" den var vi behöver den.
 */
@Service
public class PersonalityService {

    /**
     * En oföränderlig Map som kopplar personlighetsnamn till systemprompts.
     *
     * Map.of() skapar en map som INTE kan ändras efter att den skapats.
     * Det är säkert att ha som ett fält direkt i klassen eftersom den aldrig ändras.
     *
     * Nyckel (String) = det namn användaren skickar in, t.ex. "pirate"
     * Värde (String)  = den faktiska instruktionen som AI:n ska följa
     */
    private final Map<String, String> personalities = Map.of(
            "helper", "You are a friendly and helpful assistant. Answer clearly and concisely.",
            "pirate", "You are a pirate. Always respond in pirate speak. Use 'Arrr', 'matey', and similar pirate expressions.",
            "coder",  "You are a senior software engineer. Explain code concepts clearly with practical examples."
    );

    /**
     * Returnerar rätt system prompt baserat på det valda personlighetsnamnet.
     *
     * Om användaren skickar ett okänt personlighetsnamn (t.ex. "ninja") faller
     * vi tillbaka på "helper" som standard via getOrDefault().
     * Det gör att appen aldrig kraschar på grund av ett okänt värde.
     *
     * @param personality  personlighetsnamnet som kom in i ChatRequest
     * @return             system prompt-texten som ska skickas till AI:n
     */
    public String getSystemPrompt(String personality) {
        // getOrDefault() returnerar värdet för nyckeln om den finns,
        // annars returneras standardvärdet "helper"-prompten
        return personalities.getOrDefault(personality, personalities.get("helper"));
    }
}