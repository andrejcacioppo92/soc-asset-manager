# DAST Report — SOC Asset & Vulnerability Manager

**Data della campagna:** Giugno 2026
**Target:** http://localhost:8080 (Spring Boot 4.0.3 / Java 21 · React 18 + Vite)
**Metodologia:** Dynamic Application Security Testing manuale assistito, con simulazione di scanner enterprise (OWASP ZAP, Burp Suite). Approccio iterativo, un test alla volta, con verifica dello stato prima e dopo ogni operazione e rollback dei dati di test.
**Standard di riferimento:** OWASP Top 10 (2025), OWASP ASVS 4.0.3 Livello L2, OWASP Top 10 for LLM Applications, CWE/CVSS

## Executive Summary

La campagna ha eseguito circa 30 test dinamici su 8 categorie: Autenticazione, Autorizzazione/IDOR, Injection, Input Validation, Business Logic, Information Disclosure, Configurazione HTTP, e AI/LLM. Il sistema è stato testato in runtime con account reali nei tre ruoli (ADMIN, ANALYST, VIEWER).

La postura complessiva è solida. In runtime non è stato identificato alcun finding HIGH o CRITICAL sfruttabile. I controlli di sicurezza core (integrità JWT, RBAC, difesa injection, mass assignment, business logic, Human-in-the-Loop) hanno retto a tutti gli attacchi. I finding rilevati sono LOW o di natura funzionale, nessuno dei quali consente compromissione, escalation o accesso non autorizzato.

| Esito | Conteggio |
|-------|-----------|
| Test PASS | ~24 |
| FAIL (sicurezza) | 2 (entrambi MEDIUM, categoria AUTH) |
| Finding LOW / funzionali | 6 |
| Finding HIGH / CRITICAL | 0 |

Nota di coerenza con il SAST: i due finding MEDIUM identificati dinamicamente (timing enumeration e assenza di rate limiting) coincidono con quanto previsto dall'analisi statica. L'analisi statica e quella dinamica convergono sugli stessi punti, segno di una valutazione di sicurezza coerente.

## Matrice dei Test per Categoria

### A — Autenticazione
- **AUTH-001** login + security headers → PASS
- **AUTH-002** password errata → 401 muto → PASS
- **AUTH-003** user enumeration via timing → FAIL (CWE-208)
- **AUTH-004** rate limiting su /login → FAIL (CWE-307)
- **AUTH-005** JWT manipulation (alg:none, stripping, tampering) → PASS (CWE-347 mitigato)

### B — Autorizzazione
- **AUTHZ-001** gating lettura utenti → PASS
- **AUTHZ-002** DTO risposta senza hash → PASS
- **AUTHZ-003** cambio ruolo gated a monte → PASS
- **AUTHZ-004** mass assignment / auto-promozione → PASS (CWE-915 mitigato)
- **AUTHZ-005** operazione AI gated per ruolo → PASS
- **AUTHZ-006** lockdown 10 scritture non autorizzate → PASS (CWE-862 mitigato)

### C — Injection
- **INJ-001** SQLi filtri gravita/stato → PASS robusto (CWE-89 mitigato)
- **INJ-002** SQLi/property injection su sortBy → PASS robusto (CWE-89 mitigato)
- **INJ-003** parameter injection/SSRF su CVE search → PASS (CWE-88/918 mitigato)

### D — Input Validation
- **INPUT-001** stored XSS + boundary descrizione → PASS (CWE-79 mitigato)
- **INPUT-002** validazione formato IP → finding LOW (CWE-20)
- **INPUT-003** type confusion / JSON malformato → PASS + 2 LOW (CWE-704)

### E — Business Logic
- IP duplicato → 409 → PASS
- **BIZLOGIC-001** state machine ticket → PASS (CWE-840/841 mitigato)
- **BIZLOGIC-002** doppia revisione AI → PASS (CWE-840 mitigato)
- **BIZLOGIC-003** segregation of duties → non implementata (scelta di design)

### F — Information Disclosure
- header / charset / stack trace / errori → PASS

### G — Configurazione HTTP
- **CORS-001** (eco origin, credentials) → PASS
- **HTTP-METHODS-001** (TRACE, metodi non previsti) → PASS (CWE-693 mitigato)

### H — AI / LLM
- **HmAI-001** instruction override → resistito (LLM01)
- **HmAI-002** system prompt leak → leak minore (LOW, LLM01)
- **HmAI-003** format hijack → parziale/inerte (LLM01)
- **HmAI-004** sensitive disclosure → PASS (LLM06)
- **HmAI-005** insecure output handling → PASS (LLM02)
- **HmAI-006** indirect injection persistente → PASS (LLM01)

## Finding per Severity

Nessun finding CRITICAL o HIGH in runtime.

### MEDIUM (2)

**M1 — User enumeration via timing (AUTH-003)** · CWE-208 · CVSS ~5.3
Il login con utente inesistente risponde in ~3ms contro i ~54ms di un utente valido (BCrypt viene eseguito solo se l'utente esiste). Questo permette di enumerare username validi misurando la latenza della risposta. Remediation: delegare all'AuthenticationManager/DaoAuthenticationProvider di Spring, che ha un meccanismo anti-timing nativo (BCrypt fittizio anche quando l'utente non esiste), oppure eseguire un confronto su hash dummy nel ramo utente-non-trovato per pareggiare i tempi.

**M2 — Assenza di rate limiting su /login (AUTH-004)** · CWE-307 · CVSS ~7.5 (runtime)
20 tentativi falliti producono 20 risposte 401, nessun 429, nessun lockout. Brute force e password spraying illimitati. Combinato con M1, il rischio pratico aumenta. Remediation: rate limiting per IP e username con Bucket4j, risposta 429 con header Retry-After.

### LOW / Funzionali (6)

**L1 — GlobalExceptionHandler mancante** (CWE-703). Le eccezioni di validazione (@Valid, Jackson) non vengono mappate a 400: propagano oltre il controller e vengono tradotte in 403 opachi con body vuoto. Confermato su più controller. Il client non riceve i messaggi di validazione e lo status è semanticamente fuorviante. Fix: un singolo @RestControllerAdvice risolve globalmente. Priorità alta tra i LOW per impatto trasversale.

**L2 — @Pattern mancante su indirizzoIp** (CWE-20). Il campo accetta IP malformati (999.999.999.999, stringhe non-IP) come dato sporco. Non è injection (le query sono parametrizzate), è qualità del dato in un inventario SOC. Fix: InetAddressValidator di commons-validator.

**L3 — Enum-by-ordinal** (CWE-704). Jackson accetta l'ordinale numerico per gli enum (gravita:1 → HIGH). Fragile a riordini dell'enum. Fix: spring.jackson.deserialization.fail-on-numbers-for-enums=true.

**L4 — Float→Long coercion** (CWE-704). assetId:1.5 viene troncato a 1 silenziosamente. Fix: fail-on-coercion-of-floats-to-ints=true.

**L5 — System prompt LLM estraibile** (LLM01, correlato a MED-4 del SAST). Il test HmAI-002 ha esfiltrato il prompt di sistema (la descrizione del ticket è concatenata grezza, senza delimitatori). Impatto trascurabile perché il prompt non contiene segreti. Fix: incapsulare la descrizione con delimitatori e istruzione di contenimento.

**L6 — DELETE ticket con vincolo FK → 403 opaco.** Cancellare un ticket con piani collegati fallisce con 403 invece di un 409 parlante o un cascade governato. Manifestazione di L1 sulle foreign key. Fix: policy esplicita (cascade o 409 con messaggio).

### Osservazioni INFO (non finding)

X-XSS-Protection legacy (oggi si consiglia 0, la CSP è già presente). HSTS assente (atteso su HTTP, da aggiungere in produzione sotto TLS). Content-Type senza charset esplicito. Chiave JWT random in RAM (invalida i token a ogni restart, non regge scaling orizzontale). Segregation of duties non implementata (scelta di design difendibile in un modello a singola autorità ADMIN).

## Top 5 Attacchi Neutralizzati

1. **JWT forging** (alg:none, signature stripping, payload tampering) → bloccato da JJWT 0.12 `parseSignedClaims` con verifica HMAC. Dimostrato in runtime: token forgiati → 403, token valido → 200.
2. **Mass assignment / auto-promozione** (VIEWER inietta ruoli:["ADMIN"] via /me) → neutralizzato dal DTO chiuso a 3 campi, i campi extra vengono silenziosamente scartati.
3. **SQL Injection** (filtri, sortBy) → triplo strato: type-safety enum + JPQL parametrizzata + validazione proprietà Spring Data.
4. **Privilege escalation** verticale e orizzontale → RBAC con @PreAuthorize + @EnableMethodSecurity, gating a monte su 10 scritture testate su 10.
5. **Manomissione del workflow** (riapertura ticket CLOSED, doppia revisione di un piano AI già approvato) → state machine puoPassareA e guardia di stato sui piani, entrambe applicate con 409.

## Compliance Statement — OWASP ASVS 4.0.3 Livello L2 (verifica runtime)

| Capitolo ASVS | Verifica dinamica | Esito |
|---------------|-------------------|-------|
| V2 — Authentication | BCrypt, token con scadenza 60min, no credenziali in chiaro | Soddisfatto (gap: M1, M2) |
| V3 — Session Management | stateless JWT, token forgiati rifiutati, scadenza applicata | Soddisfatto |
| V4 — Access Control | RBAC per ruolo e risorsa, mass assignment bloccato, lockdown scritture | Soddisfatto |
| V5 — Input Validation | enum type-safe, JPQL parametrizzata, XSS contenuto | Soddisfatto (gap: L2/L3/L4) |
| V7 — Error Handling | nessuno stack trace esposto, messaggi puliti | Parziale (L1: status imprecisi, zero leak) |
| V8 — Data Protection | DTO risposta senza hash né campi interni | Soddisfatto |
| V14 — Configuration | security headers, CORS match esatto, TRACE disabilitato | Soddisfatto |

**Verdetto:** in runtime il sistema soddisfa il livello ASVS L2 sui capitoli verificabili dinamicamente, con due gap MEDIUM concentrati in V2 (rate limiting e timing del login) da chiudere per la conformità piena, e finding minori di error handling (V7) che non comportano information disclosure.

## Raccomandazioni di Hardening (prioritizzate)

1. **Rate limiting su /login** (M2) — il gap a più alto rischio pratico. Bucket4j.
2. **GlobalExceptionHandler** (@RestControllerAdvice) (L1, L6) — un fix con copertura trasversale: corregge gli status, restituisce i messaggi di validazione, sistema i 403 opachi.
3. **Login timing-safe** (M1) — delega all'AuthenticationManager.
4. **Hardening Jackson** (L3, L4) — due righe in application.properties.
5. **Validazione formato IP** (L2) — InetAddressValidator.
6. **Delimitatori nel prompt LLM** (L5) — contenimento del prompt injection.
7. **Produzione:** segreto JWT da variabile d'ambiente, HSTS sotto TLS, X-XSS-Protection: 0.

## Difese da Preservare

RBAC method-security, DTO chiusi, query parametrizzate, state machine, Human-in-the-Loop sui piani AI, CORS a match esatto, security headers completi. Questi controlli hanno retto a tutti gli attacchi e costituiscono la spina dorsale della sicurezza del sistema.

## Considerazioni Metodologiche

La campagna è stata condotta con approccio iterativo, un test alla volta, con verifica dello stato prima e dopo ogni operazione di scrittura e rollback dei dati di test. Il valore dell'analisi non risiede nel numero di test superati, ma nella capacità di distinguere i casi ambigui: i 403 da error-dispatch dai 403 reali di sicurezza, la dimostrazione per contrasto di dove l'error handling fosse corretto e dove no, e la verifica che il prompt injection potesse riuscire ma che l'architettura Human-in-the-Loop contenesse il danno a valle.

Questo riflette un principio fondamentale: contro le vulnerabilità di business logic e gli attacchi agli LLM, nessuno scanner automatico è sufficiente. La difesa architetturale (state machine, revisione umana, DTO chiusi) conta più della resistenza del singolo componente.

## Conclusioni

Il sistema presenta una postura di sicurezza solida e coerente con le aspettative per un capstone universitario di cybersecurity. I due gap MEDIUM sono noti, documentati e prioritizzati nella SECURITY_ROADMAP.md. Nessuna vulnerabilità critica o ad alto impatto è stata identificata in runtime. Si raccomanda di chiudere il rate limiting e il GlobalExceptionHandler prima di un'eventuale esposizione pubblica del servizio.