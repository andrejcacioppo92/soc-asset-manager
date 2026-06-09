package com.cyberdefense.assetmanager.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class NvdService {

    // endpoint pubblico del National Vulnerability Database del NIST
    // non richiede chiave per uso moderato, ottimo per cercare CVE reali
    private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";

    private final RestTemplate restTemplate = new RestTemplate();

    // cerco CVE pubbliche tramite parole chiave
    // limito i risultati a 10 così evito di sovraccaricare la chiamata
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> cercaCve(String keyword) {
        // codifico la query per gestire spazi e caratteri speciali
        String query = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        String url = NVD_BASE_URL + "?keywordSearch=" + query + "&resultsPerPage=10";

        // NVD chiede uno user agent identificativo nelle richieste
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "SOC-Asset-Manager/1.0");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("vulnerabilities")) {
                return List.of();
            }

            // estraggo solo i campi che mi servono, niente di più
            // il response NVD è enorme e voglio passare al front-end solo l'essenziale
            List<Map<String, Object>> vulnerabilities = (List<Map<String, Object>>) body.get("vulnerabilities");

            return vulnerabilities.stream()
                    .map(this::estraiDatiCve)
                    .toList();

        } catch (Exception e) {
            // se l'API NVD è giù o lenta non blocco il sistema, ritorno lista vuota
            return List.of();
        }
    }

    // estraggo i campi rilevanti da ogni CVE per costruire una risposta pulita
    @SuppressWarnings("unchecked")
    private Map<String, Object> estraiDatiCve(Map<String, Object> vuln) {
        Map<String, Object> cve = (Map<String, Object>) vuln.get("cve");
        String id = (String) cve.get("id");

        // la descrizione è un array, prendo solo la versione inglese
        List<Map<String, String>> descriptions = (List<Map<String, String>>) cve.get("descriptions");
        String descrizione = descriptions.stream()
                .filter(d -> "en".equals(d.get("lang")))
                .map(d -> d.get("value"))
                .findFirst()
                .orElse("Nessuna descrizione disponibile");

        String published = (String) cve.get("published");

        return Map.of(
                "id", id,
                "descrizione", descrizione,
                "pubblicato", published
        );
    }
}