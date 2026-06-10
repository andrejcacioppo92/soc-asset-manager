# SAST Report — SOC Asset & Vulnerability Manager

**Data dell'audit:** Giugno 2026
**Versione del progetto:** 1.0.0
**Standard di riferimento:** OWASP Top 10 (2025), OWASP ASVS 4.0.3 Livello L2, OWASP Top 10 for LLM Applications, CWE/CVSS 4.0
**Metodologia:** Static Application Security Testing assistito da AI con prompt strutturato che simula scanner enterprise (SonarQube, Checkmarx, Fortify, Veracode)

## Executive Summary

| Severity   | Count |
|------------|-------|
| CRITICAL   | 0     |
| HIGH       | 0     |
| MEDIUM     | 6     |
| LOW        | 10    |
| INFO       | 6     |

Sono stati analizzati 16 file: 4 di configurazione di sicurezza, 4 controller, 4 entity, 2 repository, 2 service, e i principali file del front-end React.

Nessuna vulnerabilità critica o ad alto impatto è stata confermata dopo le verifiche di contesto. I findings residui appartengono alla categoria del security hardening e non rappresentano vulnerabilità sfruttabili nell'attuale modello operativo (utenza interna, deployment locale, dati non sensibili).

## Quadro di Compliance

Il progetto soddisfa **parzialmente** OWASP ASVS 4.0.3 Livello L2.

**Aree pienamente coperte:**

- V3 — Session Management: architettura stateless con JWT
- V4 — Access Control: `@PreAuthorize` attivo a livello metodo (verificato), tre ruoli differenziati
- V7 — Logging & Error Handling: `SecurityLogger` capillare su tutte le operazioni sensibili
- V2.4 — Credential Storage: BCrypt confermato via `DelegatingPasswordEncoder`
- Output sicurezza AI: rendering con escaping React, nessun `dangerouslySetInnerHTML`
- State machine dei piani con anti-retroattività (`PENDING` → `APPROVED|REJECTED`, no ritorno)

**Gap residui:**

- V2.2 — Anti-automation: nessun rate limiting su `/api/auth/login`
- V6 — Key Management: chiave JWT volatile in RAM, non esternalizzata
- V14.4 — HTTP Headers: CSP minimale, da estendere
- V5 — Input Validation: enum su `gravita`/`stato` validati al livello Hibernate, non al DTO
- LLM01 — Prompt Injection: descrizione ticket concatenata nel prompt Gemini

## Findings MEDIUM Dettagliati

### MED-1 — JWT Service: Chiave di Firma Volatile

- **CWE:** CWE-320 (Key Management Errors)
- **OWASP:** A02:2025 — Cryptographic Failures
- **ASVS:** V6.4
- **CVSS 4.0:** ~5.0 (Medium)
- **File:** `src/main/java/com/cyberdefense/assetmanager/config/JwtService.java`

**Descrizione:** la chiave di firma HS256 viene generata casualmente in RAM all'avvio del server tramite `Keys.secretKeyFor(SignatureAlgorithm.HS256)`. È crittograficamente forte (non forgeable), ma volatile: ogni restart invalida tutti i token emessi, e in deployment multi-istanza ogni nodo avrebbe una chiave diversa, rompendo lo stateless.

**Impatto:** disservizio operativo (logout forzato globale a ogni restart) e impossibilità di scalare orizzontalmente.

**Remediation pianificata:** caricare la chiave da variabile d'ambiente `JWT_SECRET` (Base64, almeno 256 bit). Vedi `SECURITY_ROADMAP.md` sezione 1.

### MED-2 — JWT Service: Claim Standard Mancanti

- **CWE:** CWE-345 (Insufficient Verification of Data Authenticity), CWE-613 (Insufficient Session Expiration)
- **OWASP:** A07:2025 — Identification and Authentication Failures
- **ASVS:** V3.5
- **CVSS 4.0:** ~4.3 (Medium)
- **File:** `src/main/java/com/cyberdefense/assetmanager/config/JwtService.java`

**Descrizione:** il token JWT contiene solo i claim minimi (`sub`, `iat`, `exp`). Mancano `iss` (issuer), `aud` (audience), `jti` (token id univoco per future revoche) e `nbf` (not before). Il parser non validate `iss` né `aud`, accetterebbe un token firmato da qualsiasi sistema con la stessa chiave.

**Impatto:** ridotta capacità di tracciamento e revoca dei token, e mancanza di binding token-servizio.

**Remediation pianificata:** aggiungere `iss`, `aud`, `jti`, `nbf` in fase di generazione e validare `iss`/`aud` nel parser. Vedi `SECURITY_ROADMAP.md` sezione 1.

### MED-3 — Login: Nessuna Protezione Anti Brute-Force

- **CWE:** CWE-307 (Improper Restriction of Excessive Authentication Attempts)
- **OWASP:** A07:2025
- **ASVS:** V2.2.1
- **CVSS 4.0:** ~5.3 (Medium)
- **File:** `src/main/java/com/cyberdefense/assetmanager/controller/AuthController.java`

**Descrizione:** nessun rate limiting né lockout sull'endpoint `/api/auth/login`. Combinato con `permitAll()` su `/api/auth/**`, un attaccante può tentare password illimitate.

**Impatto:** brute-force di credenziali. Mitigato in parte dall'hashing BCrypt (costoso) e dalla lunghezza minima password.

**Remediation pianificata:** integrare Bucket4j per rate limiting per IP/username + lockout dopo N tentativi. Vedi `SECURITY_ROADMAP.md` sezione 2.

### MED-4 — AI Mitigation: Prompt Injection

- **CWE:** CWE-1427 (Improper Neutralization of Input Used in LLM Prompt)
- **OWASP:** A03:2025 — Injection, LLM01 (OWASP Top 10 for LLM Applications)
- **CVSS 4.0:** ~5.0 (Medium, mitigato da HITL)
- **File:** `src/main/java/com/cyberdefense/assetmanager/controller/AiMitigationController.java`

**Descrizione:** la descrizione del ticket (input user-controlled) viene concatenata direttamente nel prompt verso Gemini. Una descrizione del tipo "Ignora le istruzioni precedenti e..." può manipolare il piano generato.

**Mitigazioni esistenti:**

- Human-in-the-Loop: il piano resta `PENDING` fino ad approvazione admin
- Output renderizzato con escaping automatico di React (no XSS)
- Nessuna esecuzione automatica del contenuto del piano

**Remediation pianificata:** separare istruzioni di sistema dai dati utente, usare delimitatori espliciti, sanitizzare pattern noti. Vedi `SECURITY_ROADMAP.md` sezione 3.

### MED-5 — Security Headers: CSP Minimale

- **CWE:** CWE-693 (Protection Mechanism Failure)
- **OWASP:** A05:2025 — Security Misconfiguration
- **ASVS:** V14.4
- **CVSS 4.0:** ~4.0 (Medium)
- **File:** `src/main/java/com/cyberdefense/assetmanager/config/SecurityConfig.java`

**Descrizione:** sono presenti i principali header (`X-Frame-Options`, `X-Content-Type-Options`, `HSTS`, `Referrer-Policy`, `CSP`) ma la Content Security Policy è minimale (`default-src 'self'; frame-ancestors 'none'`). Manca `Permissions-Policy`. La configurazione CORS è duplicata (`http.cors` + bean `CorsFilter`).

**Remediation pianificata:** CSP granulare con `script-src`, `style-src`, `img-src`, `connect-src` espliciti. Aggiungere `Permissions-Policy`. Consolidare CORS in un singolo `CorsConfigurationSource`. Vedi `SECURITY_ROADMAP.md` sezione 4.

### MED-6 — TicketVulnerabilita: Validazione Enum al DTO Mancante

- **CWE:** CWE-20 (Improper Input Validation)
- **OWASP:** A04:2025 — Insecure Design
- **ASVS:** V5.1
- **CVSS 4.0:** ~3.5 (Low-Medium)
- **File:** `src/main/java/com/cyberdefense/assetmanager/dto/TicketDTO.java`

**Descrizione:** i campi `gravita` e `stato` sono definiti come `enum` nell'entity ma il DTO di input accetta stringhe libere. La conversione enum fallisce a livello Hibernate restituendo 400 generico, senza messaggio utente-friendly.

**Remediation pianificata:** aggiungere `@Pattern` sui campi del DTO con whitelist esplicita dei valori validi. Vedi `SECURITY_ROADMAP.md` sezione 5.

## Findings LOW (Sintesi)

I findings LOW non rappresentano vulnerabilità sfruttabili ma opportunità di hardening:

1. **SecurityConfig:** CORS `allowedHeaders` con wildcard configurazione duplicata
2. **SecurityConfig:** `permitAll()` su wildcard `/api/auth/**` (rende pubblici endpoint futuri)
3. **JwtAuthenticationFilter:** doppio parsing del token per richiesta
4. **JwtAuthenticationFilter:** stato account (enabled/locked) non riverificato a ogni richiesta
5. **JwtService:** parser senza `clockSkewSeconds`
6. **User.java:** `@ManyToMany(EAGER)` sui ruoli (impatto performance, non sicurezza)
7. **AiMitigationController:** `@RequestBody Map` generico in alcune risposte (sostituire con DTO tipizzati)
8. **Front-end:** JWT in `sessionStorage` (tradeoff documentato, mitigato da scadenza breve e CSP)
9. **Front-end:** assenza route guard client-side (la protezione vera è server-side)
10. **AuthController (prima del fix):** fallback password in chiaro come codice morto — **rimosso al termine dell'audit**

## Interventi Applicati al Termine dell'Audit

Le seguenti patch sono state implementate immediatamente dopo l'audit, in quanto a basso rischio di regressione:

1. Rimozione del fallback password in chiaro in `AuthController.login()`
2. `@JsonIgnore` sul campo `password` dell'entity `User`
3. DTO tipizzato `RevisionePianoDTO` con `@Pattern` e `@Size` per la revisione dei piani AI
4. Centralizzazione URL API nel front-end via `VITE_API_BASE_URL`
5. Retry con exponential backoff su `GeminiService` (resilienza, non security)

I gap residui sono stati documentati in `SECURITY_ROADMAP.md` con priorità, intervento pianificato e razionale del rinvio.

## Top 5 Raccomandazioni Strategiche

1. **JWT hardening completo** (chiave da env, claim standard, validazione, clock skew)
2. **Rate limiting su `/login`** con Bucket4j
3. **Prompt injection mitigation** su Gemini (separazione system/dati)
4. **HTTP Security Headers** estesi (CSP granulare, Permissions-Policy, CORS consolidato)
5. **Validazione DTO completa** (enum, GlobalExceptionHandler)

## Considerazioni Metodologiche

L'analisi è stata condotta tramite agente AI con prompt strutturato che simula scanner enterprise. Il prompt classifica ogni finding secondo Severity, CWE, OWASP Top 10, ASVS, CVSS 4.0, e fornisce remediation pronta da incollare.

Limiti dichiarati dell'agente:

- L'analisi si basa su snapshot del codice, che può differire dalla versione runtime
- L'agente ha richiesto verifica diretta dell'utente per due punti critici (`@EnableMethodSecurity` e hashing password) prima di confermare la severità dei findings, dimostrando metodologia rigorosa

Due falsi positivi iniziali sono stati identificati e dismessi grazie alla verifica con l'utente:

- **HIGH iniziale su @PreAuthorize potenzialmente inefficace:** smentito dalla presenza di `@EnableMethodSecurity` nel `SecurityConfig.java` (riga 21)
- **HIGH iniziale su password storage:** smentito dalla verifica di `ApplicationConfig.java` che usa `PasswordEncoderFactories.createDelegatingPasswordEncoder()` con BCrypt di default

Questa disciplina di anti-falso-positivo è documentata come parte della metodologia.

## Conclusioni

Il progetto presenta una postura di sicurezza coerente con le aspettative per un capstone universitario di cybersecurity. L'audit conferma l'aderenza ai principi del Secure SDLC e l'implementazione consapevole dei controlli OWASP ASVS L2.

I gap residui sono tracciati nella `SECURITY_ROADMAP.md` come backlog di sicurezza prioritizzato. Si raccomanda di chiudere i finding MEDIUM prima di un'eventuale esposizione pubblica del servizio.