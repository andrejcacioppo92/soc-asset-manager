package com.cyberdefense.assetmanager.config;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// limita i tentativi di login per IP, difesa contro brute-force e credential stuffing (CWE-307)
// uso un token bucket in memoria, una soluzione adatta a un deployment a singola istanza
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // tengo un bucket separato per ogni IP, così un attaccante non blocca gli utenti legittimi
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // massimo 5 tentativi di login al minuto per IP
    private static final int MAX_TENTATIVI = 5;
    private static final Duration FINESTRA = Duration.ofMinutes(1);

    // codice HTTP 429 Too Many Requests, restituito quando si superano i tentativi
    private static final int TROPPE_RICHIESTE = 429;

    // creo un bucket nuovo con la capacità e il refill configurati
    private Bucket creaBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(MAX_TENTATIVI).refillGreedy(MAX_TENTATIVI, FINESTRA))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();
        Bucket bucket = buckets.computeIfAbsent(ip, k -> creaBucket());

        // se ci sono ancora token consumo e proseguo, altrimenti blocco con 429
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(TROPPE_RICHIESTE);
            response.setContentType("application/json");
            response.getWriter().write("{\"errore\":\"Troppi tentativi di login, riprova tra qualche minuto\"}");
        }
    }

    // applico il filtro solo all'endpoint di login, il resto del traffico non è limitato
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/auth/login");
    }
}