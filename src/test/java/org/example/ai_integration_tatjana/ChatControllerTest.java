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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("openrouter.base-url", wireMockServer::baseUrl);
        registry.add("openrouter.api.key", () -> "test-api-key");
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void shouldReturnAiResponseForValidRequest() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personality": "pirate",
                                    "message": "Vad ar Java?",
                                    "sessionId": "test-session-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Arrr, Java ar ett programsprak, matey!"))
                .andExpect(jsonPath("$.sessionId").value("test-session-1"));
    }

    @Test
    void shouldRetryOnServerErrorAndSucceedOnThirdAttempt() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("first-failure"));

        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("first-failure")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second-failure"));

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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "personality": "helper",
                                    "message": "Testa retry",
                                    "sessionId": "retry-session"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Det fungerade efter retry!"));

        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/api/v1/chat/completions")));
    }

    @Test
    void shouldReturn503WhenAiServiceIsCompletelyDown() throws Exception {
        wireMockServer.stubFor(post(urlEqualTo("/api/v1/chat/completions"))
                .willReturn(aResponse().withStatus(503)));

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
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").exists());
    }
}