# Script di Popolazione Utenti AI Mock

## Descrizione
Questo script crea 100 utenti AI mock con itinerari di viaggio realistici per sessioni demo dell'applicazione Voyage.

## Caratteristiche Utenti AI

### Profilo
- **Email**: `ai.user.{numero}@voyage-demo.app` (da 1 a 100)
- **Password**: `VoyageAI2024!{numero}`
- **Profilo**: Sempre pubblico (`isPrivate = false`)
- **Flag**: `isAiUser = true`
- **Bio**: Descrizioni che identificano l'utente come AI Explorer
- **Avatar**: Generato automaticamente da UI Avatars

### Viaggi
- **Numero**: 3-5 viaggi per utente
- **Destinazioni**: Città famose in tutto il mondo (Parigi, Roma, Tokyo, New York, etc.)
- **Durata**: 3-7 giorni per viaggio
- **Date**: Distribuite negli ultimi 12 mesi
- **Itinerari**: 2-4 punti di interesse per giorno

### Destinazioni Disponibili
- **Europa**: Parigi, Roma, Barcellona, Amsterdam, Londra
- **Asia**: Tokyo, Dubai
- **Americhe**: New York

## Come Eseguire lo Script

### Prerequisiti
1. Database PostgreSQL configurato e accessibile
2. Firebase Admin SDK configurato con credenziali valide
3. Applicazione Spring Boot funzionante

### Passaggi

#### 1. Abilitare lo Script
Aprire il file `PopulateAiUsersScript.java` e **decommentare** la riga:

```java
// @Component // DECOMMENTARE PER ESEGUIRE LO SCRIPT
```

Diventa:

```java
@Component // DECOMMENTARE PER ESEGUIRE LO SCRIPT
```

#### 2. Avviare l'Applicazione
```bash
cd voyage-be
mvn spring-boot:run
```

Oppure se usi l'IDE, avvia normalmente l'applicazione.

#### 3. Monitorare l'Esecuzione
Lo script stamperà log dettagliati:

```
========================================
🤖 INIZIO POPOLAZIONE UTENTI AI MOCK
========================================
Creazione utente AI 1/100
✅ Utente AI creato: Marco Rossi (ai.user.1@voyage-demo.app) con 4 viaggi
...
✅ Progresso: 10/100 utenti creati
...
========================================
✅ POPOLAZIONE COMPLETATA
Utenti creati con successo: 100
Errori: 0
========================================
```

#### 4. Disabilitare lo Script
**IMPORTANTE**: Dopo l'esecuzione, **ricommentare** la riga `@Component` per evitare esecuzioni multiple:

```java
// @Component // DECOMMENTARE PER ESEGUIRE LO SCRIPT
```

## Verifica

### 1. Verifica Database
Verifica che gli utenti siano stati creati:

```sql
SELECT COUNT(*) FROM users WHERE is_ai_user = true;
-- Dovrebbe restituire 100

SELECT u.name, u.email, u.is_private, COUNT(t.id) as num_travels
FROM users u
LEFT JOIN travel t ON t.user_id = u.id
WHERE u.is_ai_user = true
GROUP BY u.id, u.name, u.email, u.is_private
LIMIT 10;

-- Verifica che siano tutti pubblici
SELECT COUNT(*) FROM users WHERE is_ai_user = true AND is_private = false;
-- Dovrebbe restituire 100
```

### 2. Script di Verifica Java
Decommenta `@Component` in `VerifyAiUsersScript.java` e riavvia l'app per vedere statistiche dettagliate.

### 3. Endpoint di Debug
Usa questi endpoint per testare (SOLO IN SVILUPPO):

```bash
# Conta utenti AI
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:9080/api/debug/ai-users-count

# Testa suggerimenti
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:9080/api/debug/test-suggestions?limit=20
```

### Firebase Authentication
Gli utenti dovrebbero essere visibili nella console Firebase Auth con email `ai.user.*@voyage-demo.app`.

### App Mobile
1. Apri l'app Voyage
2. Vai alla sezione "Suggerimenti"
3. Dovresti vedere utenti con badge "🤖 AI"
4. I profili sono pubblici e possono essere aggiunti senza richiesta

## Risoluzione Problemi

### Errore: "Firebase Auth quota exceeded"
- **Causa**: Troppi utenti creati in poco tempo
- **Soluzione**: Lo script ha già pause integrate. Se persiste, aumenta il delay nel codice.

### Errore: "User already exists"
- **Causa**: Script eseguito più volte
- **Soluzione**: 
  1. Elimina gli utenti esistenti da Firebase e DB
  2. Oppure modifica il range di indici nel loop

### Errore di connessione al Database
- **Causa**: Configurazione database errata
- **Soluzione**: Verifica `application.properties`:
  ```properties
  spring.datasource.url=${DATABASE_URL}
  spring.datasource.username=${DATABASE_USERNAME}
  spring.datasource.password=${DATABASE_PASSWORD}
  ```

## Pulizia

Per eliminare tutti gli utenti AI:

```sql
-- ATTENZIONE: Questa operazione è irreversibile!

-- 1. Elimina i viaggi
DELETE FROM travel WHERE user_id IN (
  SELECT id FROM users WHERE is_ai_user = true
);

-- 2. Elimina gli utenti dal DB
DELETE FROM users WHERE is_ai_user = true;
```

Per Firebase Auth, usa la console Firebase o uno script di cleanup separato.

## Note Tecniche

### Performance
- **Batch size**: 10 utenti alla volta con pause di 1 secondo
- **Tempo stimato**: ~10-15 minuti per 100 utenti
- **Memoria**: Ottimizzato per evitare OOM

### Transazioni
Lo script usa `@Transactional` per garantire consistenza dei dati.

### Logging
- **INFO**: Progresso generale
- **DEBUG**: Dettagli creazione singoli utenti
- **ERROR**: Errori con stack trace completo

## Supporto
Per problemi o domande, contatta il team di sviluppo o apri una issue su GitHub.