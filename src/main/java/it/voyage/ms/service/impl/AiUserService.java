package it.voyage.ms.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelType;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Service per la gestione degli utenti AI mock.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUserService {

    private final UserRepository userRepository;
    private final TravelRepository travelRepository;

    /**
     * Popola il database con utenti AI mock e i loro itinerari.
     * 
     * @param count Numero di utenti AI da creare (default: 100)
     * @return Risultato dell'operazione con statistiche
     */
    @Transactional
    public PopulationResult populateAiUsers(int count) {
        log.info("========================================");
        log.info("🤖 INIZIO POPOLAZIONE {} UTENTI AI MOCK", count);
        log.info("========================================");

        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            try {
                log.info("Creazione utente AI {}/{}", i, count);
                createAiUser(i);
                successCount++;
                
                // Pausa per evitare rate limiting Firebase
                if (i % 10 == 0) {
                    Thread.sleep(1000);
                    log.info("✅ Progresso: {}/{} utenti creati", i, count);
                }
            } catch (Exception e) {
                errorCount++;
                String errorMsg = String.format("Errore creazione utente AI %d: %s", i, e.getMessage());
                errors.add(errorMsg);
                log.error("❌ {}", errorMsg, e);
            }
        }

        log.info("========================================");
        log.info("✅ POPOLAZIONE COMPLETATA");
        log.info("Utenti creati con successo: {}", successCount);
        log.info("Errori: {}", errorCount);
        log.info("========================================");

        return new PopulationResult(successCount, errorCount, errors);
    }

    /**
     * Elimina tutti gli utenti AI dal database e da Firebase Auth.
     * 
     * @return Risultato dell'operazione con statistiche
     */
    @Transactional
    public DeletionResult deleteAllAiUsers() {
        log.info("========================================");
        log.info("🗑️ INIZIO ELIMINAZIONE UTENTI AI");
        log.info("========================================");

        // 1. Trova tutti gli utenti AI
        List<UserEty> aiUsers = userRepository.findAll().stream()
            .filter(UserEty::isAiUser)
            .toList();

        int totalUsers = aiUsers.size();
        int successCount = 0;
        int errorCount = 0;
        List<String> errors = new ArrayList<>();

        log.info("📊 Trovati {} utenti AI da eliminare", totalUsers);

        for (UserEty user : aiUsers) {
            try {
                String userId = user.getId();
                String userName = user.getName();
                String userEmail = user.getEmail();

                // 2. Elimina i viaggi dell'utente (cascade elimina anche itinerari e punti)
                List<TravelEty> userTravels = travelRepository.findByUserId(userId);
                log.debug("Eliminazione {} viaggi per utente {}", userTravels.size(), userName);
                travelRepository.deleteAll(userTravels);

                // 3. Elimina l'utente dal DB
                userRepository.delete(user);
                log.debug("Utente {} eliminato dal DB", userName);

                // 4. Elimina l'utente da Firebase Auth
                try {
                    FirebaseAuth.getInstance().deleteUser(userId);
                    log.debug("Utente {} eliminato da Firebase Auth", userName);
                } catch (FirebaseAuthException e) {
                    // Se l'utente non esiste più in Firebase, ignora l'errore
                    if (!e.getErrorCode().equals("USER_NOT_FOUND")) {
                        log.warn("Errore eliminazione Firebase per {}: {}", userName, e.getMessage());
                    }
                }

                successCount++;
                log.info("✅ Eliminato: {} ({})", userName, userEmail);

            } catch (Exception e) {
                errorCount++;
                String errorMsg = String.format("Errore eliminazione utente %s: %s", 
                    user.getName(), e.getMessage());
                errors.add(errorMsg);
                log.error("❌ {}", errorMsg, e);
            }
        }

        log.info("========================================");
        log.info("✅ ELIMINAZIONE COMPLETATA");
        log.info("Utenti eliminati con successo: {}/{}", successCount, totalUsers);
        log.info("Errori: {}", errorCount);
        log.info("========================================");

        return new DeletionResult(totalUsers, successCount, errorCount, errors);
    }

    /**
     * Ottiene statistiche sugli utenti AI.
     */
    public AiUserStats getAiUserStats() {
        long totalAiUsers = userRepository.findAll().stream()
            .filter(UserEty::isAiUser)
            .count();

        long publicAiUsers = userRepository.findAll().stream()
            .filter(u -> u.isAiUser() && !u.isPrivate())
            .count();

        long aiTravels = travelRepository.findAll().stream()
            .filter(t -> t.getUser() != null && t.getUser().isAiUser())
            .count();

        return new AiUserStats(totalAiUsers, publicAiUsers, aiTravels);
    }

    private void createAiUser(int index) throws Exception {
        // 1. Genera dati utente
        String name = generateName(index);
        String email = "ai.user." + index + "@voyage-demo.app";
        String bio = generateBio(index);

        // 2. Crea utente in Firebase Auth
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
            .setEmail(email)
            .setEmailVerified(true)
            .setPassword("VoyageAI2024!" + index)
            .setDisplayName(name)
            .setDisabled(false);

        UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(request);
        String firebaseUid = firebaseUser.getUid();

        log.debug("Firebase UID creato: {} per {}", firebaseUid, name);

        // 3. Crea entità utente nel DB
        UserEty user = new UserEty();
        user.setId(firebaseUid);
        user.setName(name);
        user.setEmail(email);
        user.setAvatar("https://ui-avatars.com/api/?name=" + name.replace(" ", "+") + "&background=random&size=200");
        user.setBio(bio);
        user.setPrivate(false);
        user.setAiUser(true);
        user.setShowEmergencyFAB(false);
        user.setCreatedAt(generateRandomPastDate(180));
        user.setLastLogin(generateRandomPastDate(30));

        userRepository.save(user);
        log.debug("Utente salvato nel DB: {}", name);

        // 4. Crea viaggi per l'utente
        int numTravels = 3 + new Random().nextInt(3);
        for (int t = 0; t < numTravels; t++) {
            createTravelForUser(user, t);
        }

        log.info("✅ Utente AI creato: {} ({}) con {} viaggi", name, email, numTravels);
    }

    private void createTravelForUser(UserEty user, int travelIndex) {
        Destination destination = getRandomDestination();
        
        TravelEty travel = new TravelEty();
        travel.setUser(user);
        travel.setTravelName(destination.cityName);
        travel.setTravelType(TravelType.SINGLE);
        travel.setIsCopied(false);
        travel.setNeedsDateConfirmation(false);

        LocalDate endDate = LocalDate.now().minusDays(new Random().nextInt(365));
        int duration = 3 + new Random().nextInt(5);
        LocalDate startDate = endDate.minusDays(duration);

        travel.setDateFrom(startDate.toString());
        travel.setDateTo(endDate.toString());

        List<DailyItineraryEty> itinerary = new ArrayList<>();
        for (int day = 1; day <= duration; day++) {
            DailyItineraryEty dailyItinerary = createDailyItinerary(travel, day, destination);
            itinerary.add(dailyItinerary);
        }
        travel.setItinerary(itinerary);

        travelRepository.save(travel);
        log.debug("Viaggio creato: {} ({} giorni)", destination.cityName, duration);
    }

    private DailyItineraryEty createDailyItinerary(TravelEty travel, int dayNumber, Destination destination) {
        DailyItineraryEty daily = new DailyItineraryEty();
        daily.setTravel(travel);
        daily.setDay(dayNumber);

        List<PointEty> points = new ArrayList<>();
        int numPoints = 2 + new Random().nextInt(3);
        
        for (int p = 0; p < numPoints; p++) {
            PointEty point = new PointEty();
            point.setDailyItinerary(daily);
            point.setOrderIndex(p);
            
            String placeName = destination.places.get(p % destination.places.size());
            point.setName(placeName);
            point.setAddress(destination.cityName + ", " + destination.country);
            
            point.setLatitude(destination.lat + (new Random().nextDouble() - 0.5) * 0.1);
            point.setLongitude(destination.lng + (new Random().nextDouble() - 0.5) * 0.1);
            
            points.add(point);
        }
        
        daily.setPoints(points);
        return daily;
    }

    private String generateName(int index) {
        String[] firstNames = {
            "Marco", "Sofia", "Luca", "Giulia", "Alessandro", "Emma", "Matteo", "Chiara",
            "Lorenzo", "Anna", "Francesco", "Valentina", "Davide", "Martina", "Andrea", "Federica",
            "Gabriele", "Sara", "Simone", "Elisa", "Tommaso", "Alessia", "Riccardo", "Francesca",
            "Pietro", "Giorgia", "Giovanni", "Beatrice", "Nicola", "Elena", "Filippo", "Alice"
        };
        
        String[] lastNames = {
            "Rossi", "Bianchi", "Ferrari", "Romano", "Colombo", "Ricci", "Marino", "Greco",
            "Bruno", "Gallo", "Conti", "De Luca", "Costa", "Giordano", "Mancini", "Rizzo"
        };
        
        Random rand = new Random(index);
        String firstName = firstNames[rand.nextInt(firstNames.length)];
        String lastName = lastNames[rand.nextInt(lastNames.length)];
        
        return firstName + " " + lastName;
    }

    private String generateBio(int index) {
        String[] bios = {
            "🤖 AI Travel Explorer | Appassionato di viaggi e scoperte",
            "✈️ Esploratore digitale | Amo condividere i miei itinerari",
            "🌍 Viaggiatore virtuale | Sempre alla ricerca di nuove mete",
            "🗺️ AI Wanderer | Creo itinerari per ispirare i tuoi viaggi"
        };
        
        return bios[index % bios.length];
    }

    private Date generateRandomPastDate(int maxDaysAgo) {
        Random rand = new Random();
        int daysAgo = rand.nextInt(maxDaysAgo);
        LocalDate date = LocalDate.now().minusDays(daysAgo);
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Destination getRandomDestination() {
        List<Destination> destinations = getAllDestinations();
        return destinations.get(new Random().nextInt(destinations.size()));
    }

    private List<Destination> getAllDestinations() {
        List<Destination> destinations = new ArrayList<>();
        
        destinations.add(new Destination("Parigi", "Francia", 48.8566, 2.3522, 
            Arrays.asList("Torre Eiffel", "Louvre", "Notre-Dame", "Arco di Trionfo")));
        destinations.add(new Destination("Roma", "Italia", 41.9028, 12.4964,
            Arrays.asList("Colosseo", "Fontana di Trevi", "Vaticano", "Pantheon")));
        destinations.add(new Destination("Barcellona", "Spagna", 41.3851, 2.1734,
            Arrays.asList("Sagrada Familia", "Park Guell", "La Rambla", "Casa Batllo")));
        destinations.add(new Destination("Amsterdam", "Paesi Bassi", 52.3676, 4.9041,
            Arrays.asList("Rijksmuseum", "Casa di Anna Frank", "Canali", "Vondelpark")));
        destinations.add(new Destination("Tokyo", "Giappone", 35.6762, 139.6503,
            Arrays.asList("Shibuya Crossing", "Senso-ji", "Tokyo Tower", "Meiji Shrine")));
        destinations.add(new Destination("New York", "USA", 40.7128, -74.0060,
            Arrays.asList("Statua della Liberta", "Central Park", "Times Square", "Brooklyn Bridge")));
        destinations.add(new Destination("Londra", "Regno Unito", 51.5074, -0.1278,
            Arrays.asList("Big Ben", "Tower Bridge", "British Museum", "London Eye")));
        destinations.add(new Destination("Dubai", "Emirati Arabi", 25.2048, 55.2708,
            Arrays.asList("Burj Khalifa", "Dubai Mall", "Palm Jumeirah", "Burj Al Arab")));
        
        return destinations;
    }
}
