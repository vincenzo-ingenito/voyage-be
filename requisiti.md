# Voyage — Microservizio Backend

## Funzionalità da implementare

### 1 — Suggerimenti luoghi vicini (Nearby Recommendations)

**Scopo:** dato un punto già presente nell'itinerario, restituire luoghi nelle vicinanze non ancora inseriti nel viaggio.

#### Classi da creare

| Classe | Tipo | Package |
|---|---|---|
| `IRecommendationService` | Interface | `it.voyage.ms.service` |
| `RecommendationService` | `@Service` | `it.voyage.ms.service.impl` |
| `RecommendationController` | `@RestController` | `it.voyage.ms.controller` |
| `NearbyPlaceDTO` | DTO (`@Data`) | `it.voyage.ms.dto.response` |
| `NearbyRecommendationResponse` | DTO (`@Data`) | `it.voyage.ms.dto.response` |

#### Costanti da aggiungere in `Constants.Google`

```java
String PLACES_NEARBY_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
int NEARBY_DEFAULT_RADIUS = 1500;
int NEARBY_MAX_RADIUS     = 5000;
int NEARBY_MAX_RESULTS    = 20;
```

#### Endpoint

```
GET /api/recommendations/nearby
```

Query parameters:

| Parametro | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `travelId` | Long | sì | — | — |
| `dayNumber` | Integer | sì | — | Campo `day` di `DailyItineraryEty` |
| `pointId` | Long | sì | — | — |
| `radius` | Integer | no | 1500 | Max 5000 |
| `type` | String | no | `tourist_attraction` | Tipo Google Places |

Risposte:

- `200` → `NearbyRecommendationResponse`
- `400` → coordinate null sul punto di riferimento
- `403` → `canUserViewTravel` restituisce `false`
- `404` → viaggio, giorno o punto non trovato

#### Schema `NearbyPlaceDTO`

| Campo | Tipo | Note |
|---|---|---|
| `placeId` | String | Google Place ID |
| `name` | String | — |
| `address` | String | — |
| `latitude` | Double | — |
| `longitude` | Double | — |
| `type` | String | Mappato in italiano con `PlaceTypeCategory` già in `PlacesService` |
| `photoReference` | String | Nullable |
| `distanceMeters` | Integer | Calcolato lato server con Haversine dal punto di riferimento |
| `alreadyInTravel` | Boolean | Sempre `false` — i duplicati vengono esclusi prima della risposta |

#### Schema `NearbyRecommendationResponse`

| Campo | Tipo | Note |
|---|---|---|
| `places` | `List<NearbyPlaceDTO>` | Ordinata per `distanceMeters` ASC |
| `referencePointName` | String | Nome del `PointEty` usato come centro |
| `referenceLatitude` | Double | — |
| `referenceLongitude` | Double | — |
| `radiusUsed` | Integer | Raggio effettivo usato |
| `totalFound` | Integer | Numero risultati dopo il filtraggio |

#### Logica del metodo `getNearbyRecommendations`

Esegui questi step nell'ordine esatto:

1. `GroupTravelService.canUserViewTravel(travelId, userId)` → se `false`, lancia eccezione 403.
2. Carica `TravelEty`, naviga fino a `DailyItineraryEty` per `dayNumber`, poi trova `PointEty` per `pointId`. Lancia `NotFoundException` se uno qualsiasi non esiste.
3. Verifica che `latitude` e `longitude` del punto non siano `null`. Se lo sono, lancia `BusinessException("Punto senza coordinate: impossibile cercare luoghi vicini")`.
4. Raccogli in un `Set<String>` tutti i `placeId` già presenti nell'intero viaggio (tutti i giorni, tutti i punti), incluso il `placeId` del punto di riferimento stesso.
5. Chiama `PlacesService.getNearbyPlaces(lat, lng, radius, type)` (metodo da aggiungere, vedi sotto).
6. Filtra i risultati escludendo i `placeId` presenti nel Set del punto 4.
7. Calcola `distanceMeters` per ogni risultato superstite con la formula Haversine.
8. Ordina per `distanceMeters` ASC e costruisci `NearbyRecommendationResponse`.

#### Estensione di `PlacesService`

Aggiungi a `IPlacesService` e implementa in `PlacesService`:

```java
@Cacheable(value = "nearbyPlaces", key = "'nearby_' + #latitude + '_' + #longitude + '_' + #radius + '_' + #type")
List<NearbyPlaceDTO> getNearbyPlaces(double latitude, double longitude, int radius, String type);
```

Usa `UriComponentsBuilder` su `PLACES_NEARBY_URL` con params `location`, `radius`, `type`, `language=it`, `key`. Deserializza la risposta con una classe interna `NearbySearchResponse` analoga a `GoogleAutocompleteResponse`. Restituisci al massimo `NEARBY_MAX_RESULTS` risultati (primo page, senza `nextPageToken`).

---

### 2 — Ottimizzazione itinerario

**Scopo:** riordinare i punti di uno o più giorni minimizzando la distanza totale percorsa. Supporta due scope: singola giornata o intero viaggio. La persistenza è opzionale e controllata dal client.

#### Classi da creare

| Classe | Tipo | Package |
|---|---|---|
| `OptimizationScope` | Enum | `it.voyage.ms.enums` (o package enums del progetto) |
| `IItineraryOptimizerService` | Interface | `it.voyage.ms.service` |
| `ItineraryOptimizerService` | `@Service` | `it.voyage.ms.service.impl` |
| `OptimizationController` | `@RestController` | `it.voyage.ms.controller` |
| `OptimizationRequest` | DTO (`@Data`) | `it.voyage.ms.dto.request` |
| `OptimizationResult` | DTO (`@Data`) | `it.voyage.ms.dto.response` |

```java
public enum OptimizationScope { SINGLE_DAY, FULL_TRAVEL }
```

#### Costante da aggiungere in `Constants`

```java
int OPTIMIZER_MAX_POINTS_PER_DAY = 20; // soglia per log warning, non blocca l'esecuzione
```

#### Endpoint

```
POST /api/optimize/{travelId}
```

| Variabile | Tipo | Note |
|---|---|---|
| `travelId` | Long | Path variable |

Request body: `OptimizationRequest` (JSON).

Risposte:

- `200` → `OptimizationResult`
- `400` → `scope = SINGLE_DAY` con `dayNumber` assente nel body
- `403` → `canUserEditTravel` restituisce `false`
- `404` → viaggio o giorno non trovato

#### Schema `OptimizationRequest`

| Campo | Tipo | Obbligatorio | Default | Note |
|---|---|---|---|---|
| `scope` | `OptimizationScope` | sì | — | `SINGLE_DAY` o `FULL_TRAVEL` |
| `dayNumber` | Integer | se `SINGLE_DAY` | — | Campo `day` di `DailyItineraryEty`. Ignorato se `FULL_TRAVEL` |
| `applyChanges` | Boolean | no | `false` | Se `true` persiste i nuovi `orderIndex` sul DB |

#### Schema `OptimizationResult`

| Campo | Tipo | Note |
|---|---|---|
| `travel` | `TravelDTO` | Costruito con `buildTravelDtoWithUrls()` se `applyChanges=true`, con `TravelMapper.convertEtyToDTO()` se `false` |
| `scope` | `OptimizationScope` | Scope applicato |
| `dayOptimized` | Integer | Valorizzato solo se `SINGLE_DAY` |
| `totalDistanceBefore` | Double | Km totali nell'ordine originale (Haversine) |
| `totalDistanceAfter` | Double | Km totali nell'ordine ottimizzato |
| `applied` | Boolean | Rispecchia `applyChanges` ricevuto |

#### Algoritmo: Nearest Neighbor

Complessità O(n²), adeguata per n ≤ 20 punti per giorno. Estrai la logica in un metodo privato statico `optimizeDay(List<PointEty> points)` per facilitare il testing unitario.

Applica questo pseudocodice a ogni `DailyItineraryEty` soggetto a ottimizzazione:

```
1. Filtra i punti con latitude != null && longitude != null → validPoints
2. Se validPoints.size() <= 1: restituisci il giorno invariato
3. current = validPoints.get(0)
   route = [current]
   visited = {current}
4. Ripeti finché route.size() < validPoints.size():
   a. nearest = punto non visitato con distanza Haversine minima da current
   b. route.add(nearest); visited.add(nearest); current = nearest
5. Aggiorna orderIndex di ogni punto in route con il suo indice 0-based
6. Punti con coordinate null: accodali in fondo a route mantenendo il loro
   ordine relativo originale, con orderIndex progressivo
```

Formula Haversine da implementare come metodo privato statico in `ItineraryOptimizerService`:

```java
private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
    final double R = 6371.0;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
             * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}
```

#### Logica del metodo `optimize(travelId, request, userId)`

Esegui questi step nell'ordine esatto:

1. `GroupTravelService.canUserEditTravel(travelId, userId)` → se `false`, lancia eccezione 403.
2. Carica `TravelEty` con `TravelRepository.findById()`. Lancia `NotFoundException` se assente.
3. Valida lo scope:
   - Se `SINGLE_DAY` e `dayNumber` è `null` → lancia `IllegalArgumentException`.
   - Se `SINGLE_DAY` e nessun `DailyItineraryEty` ha `day == dayNumber` → lancia `NotFoundException`.
4. Calcola `totalDistanceBefore` sommando le distanze Haversine tra punti consecutivi nei giorni target (solo il giorno per `SINGLE_DAY`, tutti i giorni per `FULL_TRAVEL`).
5. Applica l'algoritmo Nearest Neighbor al/ai giorno/i target. Modifica `orderIndex` dei `PointEty` in memoria. Non toccare il DB in questo step.
6. Calcola `totalDistanceAfter` con la stessa logica del punto 4, usando il nuovo ordinamento.
7. Se `applyChanges = true`: salva con `TravelRepository.save()` e costruisci il DTO con `TravelService.buildTravelDtoWithUrls()`. Se `applyChanges = false`: costruisci il DTO con `TravelMapper.convertEtyToDTO()` senza salvare.
8. Costruisci e restituisci `OptimizationResult`.

---

## Criteri di accettazione

| ID | Funzionalità | Criterio |
|---|---|---|
| AC-01 | Nearby | `GET /api/recommendations/nearby` con parametri validi → HTTP 200, lista ordinata per `distanceMeters` ASC |
| AC-02 | Nearby | Nessun `placeId` nella risposta coincide con un `placeId` già presente nel viaggio |
| AC-03 | Nearby | Punto senza coordinate → HTTP 400 con messaggio esplicativo |
| AC-04 | Nearby | Utente non autorizzato → HTTP 403 |
| AC-05 | Ottimizzazione | `scope=SINGLE_DAY`, `applyChanges=false` → HTTP 200, punti riordinati, DB invariato |
| AC-06 | Ottimizzazione | `scope=SINGLE_DAY`, `applyChanges=true` → nuovi `orderIndex` persistiti, DTO aggiornato |
| AC-07 | Ottimizzazione | `scope=FULL_TRAVEL` → ogni giorno ottimizzato indipendentemente, nessun punto spostato tra giorni |
| AC-08 | Ottimizzazione | `totalDistanceAfter` ≤ `totalDistanceBefore` sempre |
| AC-09 | Ottimizzazione | Punti con coordinate `null` accodati in fondo senza errori |
| AC-10 | Ottimizzazione | `scope=SINGLE_DAY` senza `dayNumber` → HTTP 400 |
