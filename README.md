# AI-Integrerad Spring Boot Service

En middleware-tjänst byggd i Spring Boot som fungerar som en bro mellan en användare och en AI-modell via OpenRouter. Tjänsten hanterar personligheter (system prompts), konversationsminne per session och exponerar ett REST API.

---

## Funktioner

- **REST API** — endpoint för att chatta med AI:n
- **Personligheter** — välj mellan `helper`, `pirate` och `coder`
- **Konversationsminne** — AI:n kommer ihåg tidigare meddelanden per session
- **Swagger UI** — automatisk API-dokumentation
- **Retry med backoff** — försöker igen automatiskt vid tillfälliga fel

---

## Krav

- Java 21+
- Maven (eller använd `./mvnw` som ingår i projektet)
- Ett konto på [openrouter.ai](https://openrouter.ai) för att få en API-nyckel

### AI-modell

Projektet använder **Google Gemma 4 31B** via OpenRouter (`google/gemma-4-31b-it:free`) — en gratis modell som inte kräver någon betalning.

---

## Sätt API-nyckel

Projektet använder en `.env`-fil för att hålla API-nyckeln säker och utanför GitHub.

1. Kopiera exempelfilen:
   ```bash
   cp .env.example .env
   ```

2. Öppna `.env` och ersätt platshållaren med din riktiga nyckel:
   ```
   OPENROUTER_API_KEY=din-nyckel-fran-openrouter.ai
   ```

> Din nyckel hittar du på [openrouter.ai/keys](https://openrouter.ai/keys)

### Alternativ — sätt variabeln direkt i IntelliJ

Om du inte vill använda en `.env`-fil kan du konfigurera miljövariabeln direkt i IntelliJ:

1. Gå till **Run → Edit Configurations**
2. Välj din Spring Boot-konfiguration
3. Klicka på **Modify options → Environment variables**
4. Lägg till: `OPENROUTER_API_KEY=din-nyckel-här`
5. Klicka **OK** och starta appen med den gröna play-knappen

---

## Starta appen lokalt

```bash
./mvnw spring-boot:run
```

Appen startar på `http://localhost:8080`

---

## Använd API:et

### Endpoint

```
POST http://localhost:8080/api/v1/chat
Content-Type: application/json
```

### Request body

| Fält | Typ | Beskrivning | Obligatorisk |
|------|-----|-------------|--------------|
| `personality` | String | AI:ns personlighet: `helper`, `pirate`, `coder` | Ja |
| `message` | String | Din fråga till AI:n | Ja |
| `sessionId` | String | ID för att fortsätta en konversation | Nej |

### Exempel — första meddelandet

```json
{
  "personality": "coder",
  "message": "Hur skriver jag en for-loop i Java?",
  "sessionId": "min-session-1"
}
```

### Exempelsvar

```json
{
  "reply": "En for-loop i Java skriver du såhär:\n\nfor (int i = 0; i < 10; i++) {\n    System.out.println(i);\n}",
  "sessionId": "min-session-1"
}
```

### Exempel — uppföljningsfråga (samma sessionId)

```json
{
  "personality": "coder",
  "message": "Kan du visa ett exempel med en lista istället?",
  "sessionId": "min-session-1"
}
```

> Skicka samma `sessionId` för att fortsätta konversationen — AI:n kommer ihåg vad ni pratat om.

---

## Swagger UI

Interaktiv API-dokumentation finns tillgänglig när appen är igång:

```
http://localhost:8080/swagger-ui/index.html
```

---

## Projektstruktur

```
src/main/java/.../
├── controller/
│   └── ChatController.java       # REST-endpoint POST /api/v1/chat
├── service/
│   ├── ChatService.java          # Koordinerar logiken
│   ├── MemoryService.java        # Hanterar konversationshistorik per session
│   └── PersonalityService.java   # Mappar personlighet till system prompt
├── client/
│   └── LlmClient.java            # HTTP-anrop till OpenRouter
├── config/
│   └── AppConfig.java            # Konfigurerar RestClient och aktiverar Retry
└── model/
    ├── ChatRequest.java           # Inkommande request
    ├── ChatResponse.java          # Utgående svar
    └── Message.java               # Roll + innehåll (används av AI-API:t)
```
