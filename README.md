# Realtime Chat App 💬

Questo è il mio progetto finale del corso di Full Stack: un'applicazione di chat in tempo reale fatta con Java e Spring Boot. L'idea era applicare in un solo progetto tutto quello che abbiamo visto durante il corso: backend con Spring Boot, database relazionale, autenticazione degli utenti, WebSocket per il tempo reale, invio di email, e come extra ci ho aggiunto un'integrazione con un'API di Intelligenza Artificiale che suggerisce risposte nella chat.

Non è un progetto "da produzione" né niente del genere, è un progetto di corso, quindi ci sono cose che in un ambiente reale farei diversamente (le spiego più sotto nella sezione dei miglioramenti).

## Cosa fa l'app?

- Un utente si registra con nome, email e password, e dopo può accedere.
- Dalla dashboard vede la lista delle chat già aperte e la lista degli altri utenti registrati con cui non ha ancora parlato, per iniziare una conversazione nuova.
- Dentro una chat, i messaggi vengono inviati e ricevuti **in tempo reale** (senza ricaricare la pagina) usando i WebSocket.
- C'è un pulsante "✨ Suggest" che chiede a un modello di IA (gratuito, tramite OpenRouter) di suggerire una risposta basandosi sugli ultimi messaggi della conversazione. L'utente può usare quel suggerimento, modificarlo, oppure ignorarlo — il suggerimento **non viene mai salvato nel database** finché l'utente non lo invia davvero come se lo avesse scritto lui stesso.
- C'è una pagina di statistiche (messaggi inviati, ricevuti, chat aperte) con un pulsante per farsi mandare quelle statistiche via email in formato HTML.

## Stack tecnologico

| Livello | Tecnologia |
|---|---|
| Backend | Java 17 + Spring Boot 3.3 |
| Sicurezza | Spring Security (login con form, password con hash BCrypt) |
| Database | PostgreSQL + Spring Data JPA / Hibernate |
| Viste | Thymeleaf + Bootstrap 5 (via CDN) |
| Tempo reale | WebSocket con protocollo STOMP (e SockJS come fallback se il browser non supporta WebSocket nativo) |
| IA | API di OpenRouter (modello gratuito `qwen/qwen3.8-27b:free` di default) |
| Email | `JavaMailSender` + template HTML con Thymeleaf |
| Build | Maven |

## Struttura del progetto

```
src/main/java/com/chatapp/
  config/        Configurazione di Spring: sicurezza, WebSocket, proprietà di OpenRouter
  model/         Entità JPA (User, Message)
  repository/    Interfacce di Spring Data JPA per accedere al database
  security/      Integrazione di Spring Security con i nostri utenti
  controller/    I controller: login/registrazione, viste (dashboard/chat), API REST,
                 WebSocket, suggerimenti IA e statistiche
  service/       La logica di business: utenti, messaggi, statistiche, email, IA
  dto/           Classi semplici per trasportare dati tra i livelli (form, JSON, ecc.)
src/main/resources/
  application.yml   Configurazione del database, dell'email e dell'IA (usa variabili d'ambiente)
  templates/         Le pagine HTML (Thymeleaf): login, registrazione, dashboard, chat, statistiche, email
  static/css, static/js   Stili e JavaScript del client (client STOMP, chiamate fetch, ecc.)
```

Ho cercato di mantenere una separazione chiara tra controller → service → repository, come visto a lezione, in modo che ogni livello abbia una sola responsabilità.

## Come eseguirlo sulla tua macchina

### 1. Cosa serve prima di iniziare

- JDK 17 o superiore
- Maven (oppure usare quello già integrato nel tuo IDE, come ho fatto io con IntelliJ)
- PostgreSQL in esecuzione sulla tua macchina (o raggiungibile in rete)
- Un account [OpenRouter](https://openrouter.ai/keys) per ottenere una API key gratuita (non chiede carta di credito per i modelli `:free`)
- Un account email per inviare il report delle statistiche (io ho usato Gmail con una "Password per le app")

### 2. Database

Creare un database vuoto; Hibernate si occupa di creare/aggiornare le tabelle da solo alla prima esecuzione dell'app:

```sql
CREATE DATABASE chatdb;
```

### 3. Variabili d'ambiente

L'app non ha nessuna password né API key scritta direttamente nel codice — tutto viene letto da variabili d'ambiente (questo l'ho imparato a mie spese dopo vari problemi di connessione 😅). Bisogna configurare queste prima di eseguire l'applicazione:

| Variabile | A cosa serve |
|---|---|
| `DB_USERNAME` | Utente di PostgreSQL |
| `DB_PASSWORD` | Password di PostgreSQL |
| `OPENROUTER_API_KEY` | API key di OpenRouter, perché funzioni il pulsante del suggerimento IA |
| `MAIL_USERNAME` | Utente/email del tuo account SMTP (per esempio, la tua Gmail) |
| `MAIL_PASSWORD` | Password per le app di quell'account email |
| `MAIL_FROM` | (opzionale) Indirizzo che appare come mittente dell'email delle statistiche |

Se usi IntelliJ, questo si configura in **Run/Debug Configurations → Environment variables**, tutte insieme separate da `;`, per esempio:

```
DB_USERNAME=miousername;DB_PASSWORD=miapassword;OPENROUTER_API_KEY=sk-or-v1-...;MAIL_USERNAME=tua-email@gmail.com;MAIL_PASSWORD=tua-app-password
```

### 4. Eseguirlo

```bash
mvn spring-boot:run
```

E poi aprire [http://localhost:8080](http://localhost:8080) nel browser. Ti reindirizzerà al login; da lì puoi registrare un nuovo account. Per provare la chat in tempo reale tra due utenti diversi, la cosa migliore è aprire due browser diversi (oppure uno normale e uno in modalità incognito), perché se apri due schede dello stesso browser condividono la stessa sessione e risulterai "loggato come la stessa persona" in entrambe.

## Come funziona internamente (la parte più importante)

Ho lasciato commenti in italiano nel codice per spiegare le parti chiave, ma qui riassumo in generale i pezzi che mi hanno richiesto più sforzo per capire:

- **Login e sicurezza**: Spring Security si occupa di tutto il flusso di autenticazione. La cosa più importante che ho imparato qui è che non basta definire un `DaoAuthenticationProvider`: bisogna registrarlo esplicitamente nel `SecurityFilterChain`, altrimenti Spring Boot usa per conto suo un utente "di prova" generato automaticamente e il login contro il database reale non funziona mai.
- **Chat in tempo reale**: il browser apre una connessione WebSocket verso `/ws` (con SockJS come fallback) usando la stessa sessione con cui ha già effettuato il login via HTTP, quindi non serve un login separato per il WebSocket. Quando si invia un messaggio, il server lo salva nel database e lo inoltra alla "coda" privata di entrambi gli utenti, così si aggiorna all'istante in qualsiasi scheda che abbiano aperta.
- **Suggerimento IA**: quando l'utente preme "Suggest", il backend manda a OpenRouter gli ultimi messaggi della conversazione come contesto e gli chiede di suggerire una risposta breve. Quel testo viaggia direttamente al browser nella risposta HTTP — in nessun momento viene salvato nella tabella dei messaggi. Diventa un messaggio vero solo se l'utente lo modifica/accetta e lo invia, passando per lo stesso percorso di qualsiasi messaggio scritto a mano.
- **Statistiche via email**: i conteggi vengono calcolati direttamente con query sulla tabella dei messaggi, si costruisce un'email in HTML con un template di Thymeleaf, e viene inviata con `JavaMailSender`.

## Note finali

Questo progetto mi è servito soprattutto per capire come si collegano tutti i pezzi di un'applicazione full stack reale: non è solo "farlo funzionare", ma capire perché Spring Security ha bisogno di ogni configurazione, come il WebSocket sfrutta la sessione HTTP esistente, e come tenere separata la logica di business (services) da quello che è solo gestione delle richieste (controllers).
