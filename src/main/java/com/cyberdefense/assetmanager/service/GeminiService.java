package com.cyberdefense.assetmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    // numero massimo di tentativi prima di arrendersi
    // 3 tentativi sono il giusto compromesso tra resilienza e tempo di attesa per l'utente
    private static final int MAX_TENTATIVI = 3;

    // tempo base di attesa tra un tentativo e l'altro, raddoppia ogni volta
    // pattern di exponential backoff, dà tempo al server di recuperare
    private static final long DELAY_BASE_MS = 1500;

    public GeminiService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
    }

    public String generaPianoMitigazione(String descrizioneVulnerabilita) {
        String prompt = "Sei un analista SOC senior esperto di cybersecurity. "
                + "Ti viene segnalata la seguente vulnerabilità su un asset IT: "
                + descrizioneVulnerabilita + ". "
                + "Genera un piano di mitigazione operativo in italiano, "
                + "con massimo 5 step numerati, concreti e immediatamente eseguibili. "
                + "Non aggiungere introduzioni o conclusioni, solo gli step.";

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        // riprovo fino a 3 volte se Gemini è temporaneamente sovraccarico
        // ogni tentativo aspetta più del precedente, così non martello il server
        Exception ultimoErrore = null;
        for (int tentativo = 1; tentativo <= MAX_TENTATIVI; tentativo++) {
            try {
                Map response = webClient.post()
                        .uri("/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                        .header("Content-Type", "application/json")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (response != null && response.containsKey("candidates")) {
                    List<Map> candidates = (List<Map>) response.get("candidates");
                    if (!candidates.isEmpty()) {
                        Map content = (Map) candidates.get(0).get("content");
                        List<Map> parts = (List<Map>) content.get("parts");
                        if (!parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }

                return "Errore: risposta vuota dal modello AI.";
            } catch (WebClientResponseException e) {
                ultimoErrore = e;
                // se Gemini risponde 503 (sovraccarico) o 429 (rate limit) ha senso riprovare
                // per altri errori (401, 400) non ha senso, esco subito
                boolean retryUtile = e.getStatusCode().value() == 503 || e.getStatusCode().value() == 429;
                if (!retryUtile || tentativo == MAX_TENTATIVI) {
                    return "Errore nella comunicazione con il modello AI: " + e.getMessage();
                }
                // exponential backoff: 1.5s, 3s, 6s
                try {
                    Thread.sleep(DELAY_BASE_MS * (long) Math.pow(2, tentativo - 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "Errore: operazione interrotta.";
                }
            } catch (Exception e) {
                // errori non HTTP (timeout, rete, parsing), non riprovo per evitare loop infiniti
                return "Errore nella comunicazione con il modello AI: " + e.getMessage();
            }
        }

        return "Errore nella comunicazione con il modello AI dopo " + MAX_TENTATIVI + " tentativi: "
                + (ultimoErrore != null ? ultimoErrore.getMessage() : "errore sconosciuto");
    }
}