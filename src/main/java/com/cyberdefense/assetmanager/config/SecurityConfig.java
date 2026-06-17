package com.cyberdefense.assetmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
// abilito @PreAuthorize sui metodi dei controller così posso fare hasRole('ADMIN')
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // collego il CORS all'unico CorsConfigurationSource definito sotto, niente più doppia configurazione
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // aggiungo gli HTTP Security Headers richiesti da OWASP ASVS V14.4
                // ognuno blocca una categoria specifica di attacchi lato browser
                .headers(headers -> headers
                        // blocca lo sniffing del content type, mitiga attacchi tipo MIME confusion
                        .contentTypeOptions(Customizer.withDefaults())
                        // impedisce che la pagina venga embeddata in iframe da siti terzi, blocca clickjacking
                        .frameOptions(frame -> frame.deny())
                        // attiva HSTS, forza il browser a usare HTTPS quando il sito è servito su HTTPS
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        // CSP con direttive granulari, restringo ogni tipo di risorsa a self
                        // connect-src include le origin del front-end per non bloccare le sue chiamate
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                                "script-src 'self'; " +
                                                "style-src 'self' 'unsafe-inline'; " +
                                                "img-src 'self' data:; " +
                                                "connect-src 'self' http://localhost:5173; " +
                                                "font-src 'self'; " +
                                                "object-src 'none'; " +
                                                "base-uri 'self'; " +
                                                "form-action 'self'; " +
                                                "frame-ancestors 'none'"
                                )
                        )
                        // disabilito API del browser che l'app non usa, riduce la superficie di attacco lato client
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("geolocation=(), camera=(), microphone=(), payment=()")
                        )
                        // non mando referer a siti esterni, riduce data leakage involontario
                        .referrerPolicy(ref -> ref
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                        )
                        // XSS protection legacy per browser vecchi, blocco la pagina se rileva XSS
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // unica fonte di configurazione CORS, sostituisce il vecchio bean CorsFilter separato
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        // solo gli header che il front-end usa davvero
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // evito che il browser esponga header sensibili al JS
        config.setExposedHeaders(List.of());
        // riduco i preflight tenendo in cache la policy per un'ora
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}