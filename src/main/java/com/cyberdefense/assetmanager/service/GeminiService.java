package com.cyberdefense.assetmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

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
        } catch (Exception e) {
            return "Errore nella comunicazione con il modello AI: " + e.getMessage();
        }
    }
}