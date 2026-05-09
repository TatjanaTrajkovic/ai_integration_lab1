package org.example.ai_integration_tatjana;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
// Vi importerar INTE post() från MockMvc statiskt eftersom WireMock också har en post()-metod.
// Det skulle skapa en namnkollision. Istället anropar vi MockMvcRequestBuilders.post() explicit.
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrationstester for ChatController med WireMock.
 *
 * Vad ar ett integrationstest?
 * Det testar att FLERA delar av applikationen fungerar tillsammans,
 * till skillnad fran ett enhetstest som testar en klass i isolation.
 * Har testar vi hela kedjan: Controller -> Service -> LlmClient -> (WireMock)
 *
 * Vad ar WireMock?
 * WireMock ar ett verktyg som startar en fejkad HTTP-server lokalt.
 * Vi kan saga at den: "nar du far en POST till /api/v1/chat/completions,
 * svara med det har JSON-svaret". Pa sa satt slipper vi anropa
 * det riktiga OpenRouter-API:t i tester - det gor tester snabba,
 * tillforlitliga och gratis att kora.
 *
 * @SpringBootTest         = starta hela applikationskontexten (alla @Service, @Bean osv)
 * @AutoConfigureMockMvc   = ge oss MockMvc sa att vi kan skicka HTTP-anrop i tester
 */
@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    /**
     * MockMvc ar ett testverktyg som simulerar HTTP-anrop mot vår app
     * utan att starta en riktig server. Vi kan skicka POST-anrop och
     * kontrollera svar direkt i Java-koden.
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * WireMockServer ar den fejkade externa AI-servern.
     * Den ar statisk (static) for att starta en gång for alla tester
     * istallet for att starta om vid varje testmetod.
     */
    static WireMockServer wireMockServer;

    /**
     * @BeforeAll kors EN GANG innan alla tester i klassen startar.
     * Har startar vi WireMock-servern pa en dynamisk port (slumpad ledig port).
     */
    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    /**
     * @AfterAll kors EN GANG nar alla tester i klassen ar klara.
     * Har stanger vi WireMock-servern sa att porten frigors.
     */
    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    /**
     * @DynamicPropertySource gor att vi kan overskriva properties i tester.
     *
     * Har sager vi at Spring:
     *   - Anvand WireMock-serverns URL som base-URL istallet for openrouter.ai
     *   - Anvand en fejk-API-nyckel (WireMock bryr sig inte om nyckeln)
     *
     * Pa sa satt gar alla HTTP-anrop fran LlmClient till WireMock
     * istallet for till det riktiga OpenRouter.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Peka om base-URL till WireMocks lokala server
        registry.add("openrouter.base-url", wireMockServer::baseUrl);
        // Satt en fejk-API-nyckel (kravs av @Value men WireMock ignorerar den)
        registry.add("openrouter.api.key", () -> "test-api-key");
    }

    /**
     * @BeforeEach kors FORE VARJE testmetod.
     * Vi nollstaller WireMock sa att stubbar fran ett test inte
     * pverkar nasta test.
     */
    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    // =========================================================================
    // TESTFALL 1: Normalt anrop fungerar
    // =========================================================================

    /**
     * Testar att ett giltigt anrop returnerar AI:ns svar korrekt.
     *
     * Scenario: Anvandaren skickar en fraga med personality "pirate".
     * WireMock svarar med ett fejkat AI-svar.
     * Vi verifierar att appen returnerar HTTP 200 och ratt svartext.
     */
    @Test
    void shouldReturnAiResponseForValidRequest() throws Exception {

        // GIVEN: WireMock ar konfigurerad att svara med ett giltigt AI-svar
        // stubFor sager: "nar du ser det har anropet, svara sa har"
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        // Det har ar det JSON-format som OpenRouter anvander i sina svar
                        .withBody("""
                                {
                                    "choices": [
                                        {
                                            "message": {
                                                "role": "assistant",
                                                "content": "Arrr, Java ar ett programsprak, matey!"
                                            }
                                        }
                                    ]
                                }
                                """)));

        // WHEN: Vi skickar ett POST-anrop till var endpoint
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personality": "pirate",
                                    "message": "Vad ar Java?",
                                    "sessionId": "test-session-1"
                                }
                                """))

                // THEN: Vi forväntar oss HTTP 200 och ratt innehall i svaret
                // andExpect = "kontrollera att svaret uppfyller det har kravet"
                // jsonPath("$.reply") = plocka ut "reply"-faltet fran JSON-svaret
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Arrr, Java ar ett programsprak, matey!"))
                .andExpect(jsonPath("$.sessionId").value("test-session-1"));
    }

    // =========================================================================
    // TESTFALL 2: Retry-logiken fungerar (VG)
    // =========================================================================

    /**
     * Testar att @Retryable fungerar korrekt nar OpenRouter ar nere.
     *
     * Scenario: OpenRouter svarar med 503 (Service Unavailable) de forsta
     * tva gangerna, men lyckas pa tredje forsöket.
     * Vi verifierar att appen forsöker igen automatiskt och till slut
     * returnerar ett lyckat svar.
     *
     * WireMock "Scenarios" ar ett satt att simulera tillstandsforandringar:
     * "forsta gangen -> gor A, andra gangen -> gor B, tredje gangen -> gor C"
     *
     * OBS: Det har testet tar ca 3 sekunder extra pa grund av backoff-fordrojningen
     * (1 sekund + 2 sekunder mellan forsoken). Det ar avsiktligt.
     */
    @Test
    void shouldRetryOnServerErrorAndSucceedOnThirdAttempt() throws Exception {

        // GIVEN: WireMock ar konfigurerad att misslyckas tva ganger, sedan lyckas

        // Forsok 1: returnera 503
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")      // Startlage
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("first-failure"));   // Ga till nasta tillstand

        // Forsok 2: returnera 503 igen
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("first-failure")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-failure"));

        // Forsok 3: returnera 200 med ett lyckat svar
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("second-failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "choices": [
                                        {
                                            "message": {
                                                "role": "assistant",
                                                "content": "Det fungerade efter retry!"
                                            }
                                        }
                                    ]
                                }
                                """)));

        // WHEN: Vi skickar ett anrop
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personality": "helper",
                                    "message": "Testa retry",
                                    "sessionId": "retry-session"
                                }
                                """))

                // THEN: Appen ska ha lyckats pa tredje forsöket
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Det fungerade efter retry!"));

        // Verifiera att WireMock fick exakt 3 anrop (1 original + 2 retry)
        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/v1/chat/completions")));
    }

    // =========================================================================
    // TESTFALL 3: Felhantering nar AI:n ar helt nere (VG)
    // =========================================================================

    /**
     * Testar att GlobalExceptionHandler returnerar ett snyggt felmeddelande
     * nar OpenRouter ar helt nere (alla 3 forsok misslyckas).
     *
     * Scenario: WireMock svarar alltid med 503.
     * @Retryable forsöker 3 ganger, ger upp, kastar HttpServerErrorException.
     * GlobalExceptionHandler fangar det och returnerar HTTP 503 med JSON-felmeddelande.
     */
    @Test
    void shouldReturn503WhenAiServiceIsCompletelyDown() throws Exception {

        // GIVEN: WireMock svarar alltid med 503 (ingen retry hjalper)
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse().withStatus(503)));

        // WHEN & THEN: Vi forväntar oss HTTP 503 och ett felmeddelande
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personality": "helper",
                                    "message": "Ar nagon hemma?",
                                    "sessionId": "down-session"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                // Kontrollera att felmeddelandet innehaller "status"-faltet
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").exists());
    }
}
