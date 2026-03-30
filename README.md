# SOC Asset & Vulnerability Manager

Dashboard per Security Operations Center (SOC) con gestione inventario asset IT, ticketing vulnerabilità e piani di mitigazione generati da Intelligenza Artificiale.

## Stack Tecnologico

- **Back-End:** Java 21, Spring Boot 4.0.3, Spring Security, Spring Data JPA
- **Database:** PostgreSQL
- **Autenticazione:** JWT (HS256, chiave generata in RAM)
- **AI Engine:** Google Gemini 2.5 Flash API
- **Front-End:** React 18, Vite, Tailwind CSS

## Architettura di Sicurezza (OWASP ASVS 4.0.3 - Livello L2)

- Autenticazione stateless con JWT e chiave crittografica generata in RAM
- Validazione rigorosa degli input tramite DTO con Bean Validation
- Protezione IDOR su ogni endpoint tramite SecurityContextHolder
- Password hashate con BCrypt (DelegatingPasswordEncoder)
- CORS ristretto a origin e header specifici
- Gestione errori sicura senza information leakage
- Logging strutturato di ogni operazione sensibile (accessi, modifiche, violazioni)
- Nessuna credenziale nel codice sorgente (variabili d'ambiente)

## Prerequisiti

- Java 21+
- PostgreSQL 15+
- Node.js 18+ (per il Front-End)
- Un account Google AI Studio per la chiave Gemini API

## Configurazione

1. Clona il repository

2. Crea il database PostgreSQL:
```
createdb cyber_asset_db
```

3. Copia il file di esempio delle variabili d'ambiente:
```
cp .env.example .env
```

4. Modifica `.env` con le tue credenziali

5. Imposta le variabili d'ambiente (PowerShell):
```powershell
$env:DB_USERNAME="il_tuo_username"
$env:DB_PASSWORD="la_tua_password"
$env:GEMINI_API_KEY="la_tua_chiave_gemini"
```

6. Avvia il server:
```
.\mvnw spring-boot:run
```

Il server sarà disponibile su `http://localhost:8080`

## API Endpoints

### Autenticazione
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| POST | /api/auth/login | Login e generazione token JWT |

### Asset
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | /api/assets | Lista tutti gli asset |
| GET | /api/assets/{id} | Dettaglio singolo asset |
| POST | /api/assets/servers | Crea un nuovo server |
| POST | /api/assets/firewalls | Crea un nuovo firewall |
| DELETE | /api/assets/{id} | Elimina un asset |

### Ticket Vulnerabilità
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | /api/tickets | Lista tutti i ticket |
| GET | /api/tickets/{id} | Dettaglio singolo ticket |
| GET | /api/tickets/asset/{assetId} | Ticket di un asset specifico |
| POST | /api/tickets | Crea un nuovo ticket |
| DELETE | /api/tickets/{id} | Elimina un ticket |

### AI Mitigation
| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | /api/ai/mitigate/{ticketId} | Genera piano di mitigazione AI |

Tutti gli endpoint (eccetto login) richiedono il token JWT nell'header `Authorization: Bearer <token>`.

## Struttura del Progetto
```
src/main/java/com/cyberdefense/assetmanager/
├── config/          # Sicurezza, JWT, logging
├── controller/      # Endpoint REST
├── dto/             # Oggetti di trasferimento (input/output)
├── entity/          # Entità JPA (AssetIT, Server, Firewall, Ticket, User, Role)
├── repository/      # Interfacce Spring Data
└── service/         # Logica di business
```

## Standard di Riferimento

- OWASP Top 10 (2025)
- OWASP ASVS 4.0.3 (Livello L2)
- NIST Cybersecurity Framework
- JWT Security Best Practices (Philippe De Ryck)

## Note di Sicurezza

Le credenziali del database e la chiave API Gemini non sono incluse nel repository. Ogni sviluppatore deve configurare le proprie variabili d'ambiente seguendo il file `.env.example`. In un ambiente di produzione si consiglia l'uso di un secret manager (es. HashiCorp Vault, AWS Secrets Manager).