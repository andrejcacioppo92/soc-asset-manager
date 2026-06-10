# Security Roadmap

Questo documento traccia lo stato della postura di sicurezza del progetto SOC Asset & Vulnerability Manager. Riassume i risultati dell'audit SAST condotto sul codice sorgente, gli interventi di hardening già applicati e gli interventi pianificati per le prossime iterazioni di sviluppo.

L'approccio adottato è quello di un ciclo iterativo: identificare le vulnerabilità tramite scanner statici, applicare le patch a basso rischio nell'immediato, e documentare le restanti come backlog di sicurezza con priorità e razionale tecnico.

## Stato attuale della compliance

Il progetto ha come target di riferimento **OWASP ASVS 4.0.3 Livello L2**.

Dopo l'audit SAST e i fix applicati, la postura risulta:

- **Soddisfatto:** V3 (architettura stateless), V4 (access control per ruolo a livello metodo), V7 (logging di sicurezza), V2.4 (hashing BCrypt delle password), buona gestione dell'output AI lato front-end, state machine dei piani con anti-retroattività.
- **Gap residui:** V2.2 (anti-automation), V6 (key management JWT), V14.4 (header HTTP), V5 (validazione enum su alcuni campi), LLM01 (sanitizzazione prompt).

Risultato sintetico dell'audit:

- CRITICAL: 0
- HIGH: 0 confermati
- MEDIUM: 6 (documentati di seguito)
- LOW: 10 circa
- INFO/advisory: 6 circa

## Interventi già applicati

Le seguenti patch sono state implementate al termine dell'audit, in quanto a basso rischio di regressione e ad alto impatto sulla sicurezza:

1. **Rimozione del fallback password in chiaro** in `AuthController.login()`. Il confronto password si affida ora esclusivamente a `BCryptPasswordEncoder.matches()`. Codice morto eliminato (CWE-256, CWE-208).
2. **Annotazione `@JsonIgnore` sul campo `password` dell'entity `User`**. Difesa in profondità per evitare leak accidentali del campo nelle response JSON, anche se l'architettura DTO già lo impedisce a monte (CWE-200).
3. **DTO tipizzato per la revisione dei piani AI** (`RevisionePianoDTO`) con `@Pattern` whitelist sulla decisione (APPROVED/REJECTED) e `@Size(max=500)` sulle note. Sostituisce la `Map` generica precedente nel controller (CWE-20, ASVS V5.1.3).
4. **Centralizzazione dell'URL API nel front-end** tramite `import.meta.env.VITE_API_BASE_URL` con fallback su localhost. Eliminate le URL hardcoded da 6 file React, ora il deployment può cambiare ambiente senza modifiche al codice (CWE-547).
5. **Retry con exponential backoff** sulle chiamate a Google Gemini in `GeminiService`. Gestisce 503 e 429 con tre tentativi (1.5s, 3s, 6s) prima di propagare l'errore. Migliora la resilienza in caso di sovraccarico temporaneo del provider AI.

## Backlog di sicurezza

I seguenti interventi sono pianificati per le prossime iterazioni di sviluppo. Sono stati identificati durante l'audit e classificati come "improvements" piuttosto che "fix urgenti": non rappresentano vulnerabilità sfruttabili in produzione nell'attuale modello operativo (utenza interna, deployment locale, dati non sensibili), ma costituiscono debt di sicurezza da chiudere prima di un'esposizione pubblica del servizio.

### 1. JWT hardening completo

**Priorità:** Alta

**Finding di riferimento:** SAST MED-1 (CWE-320), SAST MED-2 (CWE-345, CWE-613)

**Stato attuale:** la chiave HS256 è generata casualmente in RAM all'avvio del server. È crittograficamente forte (non forgeable) ma volatile: ogni restart invalida tutti i token emessi, e in deployment multi-istanza ogni nodo avrebbe una chiave diversa rompendo lo stateless. Inoltre il token contiene solo i claim minimi (`sub`, `iat`, `exp`).

**Intervento pianificato:**
- Caricare la chiave da variabile d'ambiente `JWT_SECRET` (Base64, almeno 256 bit)
- Aggiungere i claim standard: `iss` (issuer), `aud` (audience), `jti` (token id univoco), `nbf` (not before)
- Validare `iss` e `aud` nel parser
- Aggiungere `clockSkewSeconds(30)` per tolleranza tra server con clock leggermente disallineati
- Consolidare il parsing in un singolo punto per evitare doppio parsing nel filtro

**Ragione del rinvio:** intervento che tocca un punto critico (la firma del token) e richiede invalidazione globale delle sessioni. Va fatto in una finestra di manutenzione concordata, non in coda a un audit.

### 2. Rate limiting sull'endpoint di login

**Priorità:** Alta

**Finding di riferimento:** SAST MED-3 (CWE-307)

**Stato attuale:** nessun limite al numero di tentativi di login. Un attaccante con accesso di rete al back-end può tentare brute-force illimitati.

**Intervento pianificato:**
- Integrare la libreria Bucket4j per il rate limiting
- Configurare 5 tentativi per IP per minuto sull'endpoint `/api/auth/login`
- Aggiungere lockout temporaneo dell'account dopo N tentativi falliti consecutivi
- Loggare i tentativi sospetti per analisi successive

**Ragione del rinvio:** richiede una nuova dipendenza Maven e una configurazione di stato condivisa (necessaria per il rate limiting in eventuali deployment multi-istanza). Da valutare se usare Bucket4j in-memory o backato da Redis.

### 3. Mitigazione Prompt Injection su Gemini

**Priorità:** Media

**Finding di riferimento:** SAST MED-4 (CWE-1427, OWASP LLM01)

**Stato attuale:** la descrizione del ticket (input user-controlled) viene concatenata direttamente nel prompt inviato a Gemini. Una descrizione malevola del tipo "ignora le istruzioni precedenti e..." può potenzialmente manipolare il piano generato.

**Mitigazioni già presenti:** Human-in-the-Loop (l'admin approva il piano prima di considerarlo attivo), output renderizzato con escaping React (no XSS).

**Intervento pianificato:**
- Separare nettamente le istruzioni di sistema dai dati utente nel prompt
- Inserire delimitatori espliciti attorno al contenuto del ticket (es. `<<TICKET_DATA>> ... <<END_TICKET_DATA>>`)
- Istruire il modello a trattare il contenuto delimitato come dati e non come istruzioni
- Eventualmente sanitizzare la descrizione rimuovendo pattern noti di prompt injection prima dell'invio

**Ragione del rinvio:** richiede test di efficacia contro payload di prompt injection, da fare in una sessione dedicata di adversarial testing.

### 4. HTTP Security Headers completi

**Priorità:** Media

**Finding di riferimento:** SAST MED-5 (CWE-693, ASVS V14.4)

**Stato attuale:** alcuni header sono già presenti (`X-Frame-Options`, `X-Content-Type-Options`, `HSTS`, `Referrer-Policy`, `CSP`), ma la Content Security Policy attuale è minimale (`default-src 'self'; frame-ancestors 'none'`).

**Intervento pianificato:**
- Estendere la CSP con direttive granulari: `script-src`, `style-src`, `img-src`, `connect-src`
- Aggiungere `Permissions-Policy` per disabilitare geolocation, camera, microfono
- Consolidare la configurazione CORS in un unico `CorsConfigurationSource`, evitando la doppia configurazione attuale (`http.cors` + bean `CorsFilter`)

**Ragione del rinvio:** intervento estetico nella gestione (consolidamento CORS) che non aggiunge sicurezza ma migliora la manutenibilità. La CSP estesa va testata nel browser per evitare di bloccare risorse legittime.

### 5. Validazione enum su gravità e stato dei ticket

**Priorità:** Bassa

**Finding di riferimento:** SAST MED-6 (CWE-20, ASVS V5.1)

**Stato attuale:** i campi `gravita` e `stato` di `TicketVulnerabilita` sono `enum` nell'entity, ma la validazione del DTO accetta stringhe libere prima della conversione.

**Intervento pianificato:**
- Aggiungere `@Pattern(regexp = "CRITICAL|HIGH|MEDIUM|LOW")` sul DTO di creazione ticket
- Aggiungere `@Pattern(regexp = "OPEN|IN_PROGRESS|RESOLVED|CLOSED")` sul DTO di cambio stato (già presente nel relativo controller)
- Restituire 400 Bad Request con messaggio chiaro in caso di valore non valido

**Ragione del rinvio:** il sistema funziona correttamente perché la conversione enum lato Hibernate fallisce con 400 in caso di valore non valido, ma il messaggio di errore non è ideale per il client.

### 6. Global Exception Handler

**Priorità:** Bassa

**Stato attuale:** le eccezioni sono gestite localmente nei controller con try/catch. Funziona ma è ridondante.

**Intervento pianificato:**
- Implementare un `@RestControllerAdvice` con `GlobalExceptionHandler`
- Centralizzare la gestione di `MethodArgumentNotValidException`, `AccessDeniedException`, `EntityNotFoundException`, `DataIntegrityViolationException`
- Restituire response uniformi e sanificate (senza stack trace) come da ASVS V7.4

**Ragione del rinvio:** refactoring puramente architetturale, non chiude vulnerabilità ma migliora la pulizia del codice.

## Note sulla metodologia di sicurezza

Il progetto è stato sviluppato applicando consapevolmente i principi del **Secure SDLC**: la sicurezza è considerata un requisito non funzionale fin dalle prime iterazioni e non un'attività di fine progetto.

Sono stati seguiti come riferimento:

- **OWASP Top 10 (2025)** per identificare le categorie di rischio principali
- **OWASP ASVS 4.0.3 Level L2** come baseline di controlli da implementare
- **OWASP Top 10 for LLM Applications** per la parte di integrazione con Gemini
- **NIST Cybersecurity Framework** nei pilastri Identify, Protect, Detect, Respond
- **JWT Security Best Practices** (Philippe De Ryck) per la gestione dei token

Un episodio significativo dello sviluppo è stato il commit accidentale di una chiave Gemini API nelle prime fasi del progetto: la chiave è stata rilevata da GitHub Secret Scanning, Google Cloud e GitGuardian nell'arco di pochi minuti, immediatamente revocata e rigenerata. L'incidente ha rafforzato l'adozione di `.gitignore`, `.env.example` con placeholder e script di avvio esclusi dal versionamento.

## Strumenti utilizzati per l'analisi

Per l'audit SAST è stata utilizzata un'analisi assistita da AI con prompt strutturato che simula il comportamento di scanner enterprise (SonarQube, Checkmarx, Fortify, Veracode), classificando ogni finding secondo:

- Severity (CRITICAL/HIGH/MEDIUM/LOW/INFO)
- CWE ID
- OWASP Top 10 (2025)
- OWASP ASVS 4.0.3
- CVSS 4.0 stimato

L'approccio è descritto nel documento di prompt engineering allegato al portfolio del progetto.

## Conclusioni

Il progetto soddisfa **parzialmente** OWASP ASVS 4.0.3 Livello L2, con una postura coerente con le aspettative per un progetto universitario di portfolio. I gap residui sono documentati e prioritizzati. Nessuna vulnerabilità critica o ad alto impatto è stata identificata dopo gli interventi di hardening.