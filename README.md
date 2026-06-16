# SOC Asset & Vulnerability Manager — Back-End

![CI](https://github.com/andrejcacioppo92/soc-asset-manager/actions/workflows/ci.yml/badge.svg)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=andrejcacioppo92_soc-asset-manager&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=andrejcacioppo92_soc-asset-manager)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=andrejcacioppo92_soc-asset-manager&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=andrejcacioppo92_soc-asset-manager)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=andrejcacioppo92_soc-asset-manager&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=andrejcacioppo92_soc-asset-manager)

Dashboard per Security Operations Center (SOC) con gestione inventario asset IT, ticketing vulnerabilità e piani di mitigazione generati da Intelligenza Artificiale.

Progetto capstone full-stack per il corso di Cybersecurity presso EPICODE Institute of Technology.

## Panoramica

In un SOC la prima difesa è sapere cosa proteggere. Questo back-end espone le API REST per gestire l'inventario degli asset IT aziendali (server e firewall), aprire ticket di vulnerabilità con livelli di gravità e workflow di stato, e richiedere a Google Gemini un piano di mitigazione contestualizzato sui dati reali dell'asset. Tutti i piani AI passano per una fase di revisione umana (Human-in-the-Loop) prima di essere considerati approvati.

## Stack Tecnologico

- **Java 21** + **Spring Boot 4.0.6**
- **Spring Security** con autenticazione **JWT stateless (HS256)**
- **PostgreSQL** con **Spring Data JPA** e **Hibernate**
- **Google Gemini 2.5 Flash** per la generazione dei piani di mitigazione
- **NVD NIST API** per la ricerca di CVE pubbliche
- **Maven** come build tool
- **BCrypt** per l'hashing delle password
- **Docker** + **Docker Compose** per la containerizzazione

## Architettura

Architettura a tre strati con rigorosa separazione delle responsabilità:

- **Controller**: solo smistamento HTTP, validazione DTO, gestione status code
- **Service**: tutta la logica di business, mai accessibile direttamente dai controller
- **Repository**: Spring Data JPA, accesso al database con prepared statements

Le Entity JPA non escono mai dal Service layer. Tutto ciò che entra ed esce dai Controller passa per DTO con validazione esplicita.

## Sicurezza (OWASP ASVS 4.0.3 — Livello L2)

- Autenticazione JWT stateless con chiave HS256 generata in RAM
- Token con scadenza 1 ora, mai persistito su disco
- Password hashate con BCrypt (DelegatingPasswordEncoder)
- 3 ruoli con permessi differenziati: ADMIN, ANALYST, VIEWER
- `@PreAuthorize` su tutti gli endpoint sensibili
- Validazione DTO con Bean Validation (`@Valid`, `@NotBlank`, `@Size`)
- State machine sui ticket per evitare transizioni illegali
- Vincolo IP unico sugli asset, sia a livello DB che applicativo
- Human-in-the-Loop sull'AI: piani salvati come PENDING fino ad approvazione manuale
- CORS ristretto a origin e header specifici
- HTTP Security Headers: CSP, X-Frame-Options, X-Content-Type-Options, HSTS, Referrer-Policy
- Logging strutturato di ogni operazione sensibile (accessi, modifiche, violazioni)
- Nessuna credenziale nel codice sorgente, tutto via variabili d'ambiente

## Modello Dati

9 tabelle con relazioni coerenti e una struttura di ereditarietà:

- `users` + `roles` + `user_roles` per l'autenticazione e l'autorizzazione
- `asset_it` (astratta) con `server` e `firewall` come sottoclassi (`InheritanceType.JOINED`)
- `ticket_vulnerabilita` con relazione `@ManyToOne` verso `asset_it`
- `piani_mitigazione` per i piani AI in attesa di revisione
- `audit_log` per la persistenza degli eventi di sicurezza
- `notifications` per le notifiche operative

## Endpoint REST

### Autenticazione
- `POST /api/auth/login` — login e generazione JWT

### Asset Inventory
- `GET /api/assets` — lista asset (tutti i ruoli)
- `GET /api/assets/{id}` — dettaglio asset (tutti i ruoli)
- `POST /api/assets/servers` — crea server (ADMIN)
- `POST /api/assets/firewalls` — crea firewall (ADMIN)
- `DELETE /api/assets/{id}` — elimina asset (ADMIN)

### Ticket Vulnerabilità
- `GET /api/tickets?gravita=&stato=&sortBy=&direction=` — lista con filtri e sorting
- `GET /api/tickets/stats` — aggregazioni per gravità e stato
- `GET /api/tickets/{id}` — dettaglio ticket
- `GET /api/tickets/asset/{assetId}` — ticket di un asset specifico
- `POST /api/tickets` — crea ticket (ADMIN, ANALYST)
- `PATCH /api/tickets/{id}/stato` — cambia stato con state machine (ADMIN, ANALYST)
- `DELETE /api/tickets/{id}` — elimina ticket (ADMIN)

### AI Mitigation
- `GET /api/ai/mitigate/{ticketId}` — genera piano contestualizzato (ADMIN, ANALYST)
- `GET /api/ai/piani/pending` — piani in attesa di revisione (ADMIN)
- `PATCH /api/ai/piani/{id}/revisione` — approva o rifiuta piano (ADMIN)

### CVE Search
- `GET /api/cve/search?keyword=` — cerca CVE reali su NVD NIST

### User Management
- `GET /api/users/me` — profilo utente corrente
- `PATCH /api/users/me` — aggiorna proprio profilo
- `GET /api/users` — lista utenti (ADMIN)
- `POST /api/users/registra` — registra nuovo utente (ADMIN)
- `PATCH /api/users/{id}/ruolo` — cambia ruolo (ADMIN)

## Prerequisiti

- Java 21+
- PostgreSQL 15+
- Maven (usa il wrapper `mvnw` incluso, non serve installare globalmente)
- Account Google AI Studio per la chiave Gemini API ([crea qui](https://aistudio.google.com/apikey))

## Configurazione (avvio locale)

1. Clona il repository
2. Crea il database PostgreSQL eseguendo: `CREATE DATABASE cyber_asset_db;`
3. Imposta le variabili d'ambiente necessarie all'avvio:

- `DB_USERNAME` — utente del database PostgreSQL
- `DB_PASSWORD` — password del database
- `GEMINI_API_KEY` — chiave API di Google Gemini
- `DB_URL` (opzionale) — URL JDBC del database, default `jdbc:postgresql://localhost:5432/cyber_asset_db`

Per comodità è incluso uno script `start-dev.ps1` (gitignorato) che carica le variabili e avvia l'applicazione. È disponibile un file `.env.example` come modello.

4. Avvia l'applicazione con il comando `.\mvnw spring-boot:run` — il back-end sarà disponibile su `http://localhost:8080`.

## Avvio con Docker

Il progetto è completamente containerizzato. Con Docker è possibile avviare back-end e database PostgreSQL insieme, senza installare Java o PostgreSQL in locale.

### Prerequisiti
- Docker Desktop installato e in esecuzione

### Configurazione
Copia il file di esempio con `copy .env.docker.example .env.docker`, poi apri `.env.docker` e imposta le credenziali del database e la chiave API di Google Gemini. Questo file è escluso da Git e non viene mai pubblicato.

### Avvio
Avvia tutto con il comando `docker compose --env-file .env.docker up --build`. Il comando costruisce l'immagine del back-end (multi-stage build) e avvia due container: il database PostgreSQL e l'applicazione Spring Boot. Il back-end attende che il database sia pronto (healthcheck) prima di avviarsi, e lo raggiunge tramite la rete interna di Docker. Una volta avviato, il back-end è disponibile su `http://localhost:8080`.

### Arresto
Ferma e rimuovi i container con `docker compose --env-file .env.docker down`. I dati del database persistono in un volume Docker dedicato tra un riavvio e l'altro.

### Note di sicurezza
- L'immagine usa un multi-stage build: la fase di compilazione (Maven + JDK) è separata dalla fase di runtime (solo JRE), riducendo dimensione e superficie d'attacco.
- L'applicazione gira come utente non-root dedicato all'interno del container.
- I segreti non sono mai inclusi nell'immagine né nel codice: vengono iniettati a runtime tramite variabili d'ambiente.

## Sicurezza della Supply Chain (CI/CD)

Il repository integra una pipeline di sicurezza continua:

- **CI con GitHub Actions**: build e test automatici a ogni push, con PostgreSQL containerizzato come service
- **SonarCloud**: analisi statica (SAST) automatica a ogni push, Security Rating A
- **Dependabot**: scanning continuo delle dipendenze (SCA) con aggiornamenti automatici delle vulnerabilità note

## Licenza

Progetto realizzato a scopo didattico nell'ambito del percorso di Cybersecurity presso EPICODE Institute of Technology.