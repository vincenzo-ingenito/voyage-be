package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.TravelEty;

@Repository
public interface TravelRepository extends JpaRepository<TravelEty, Long> {

	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			WHERE t.user.id = :userId
			""")
	List<TravelEty> findByUserId(@Param("userId") String userId);

	Optional<TravelEty> findByIdAndUserId(Long id, String userId);


	// Query 3: files
	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.files
			WHERE t.id = :id AND t.user.id = :userId
			""")
	Optional<TravelEty> findByIdAndUserIdWithFiles(
			@Param("id") Long id, @Param("userId") String userId);


	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.itinerary i
			LEFT JOIN FETCH i.points
			WHERE t.user.id = :userId
			""")
	List<TravelEty> findByUserIdWithPoints(@Param("userId") String userId);

	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.files
			WHERE t.user.id = :userId
			""")
	List<TravelEty> findByUserIdWithFiles(@Param("userId") String userId);


	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.user
			WHERE t.user.id = :userId
			OR t.user.id IN :friendIds
			ORDER BY t.dateTo DESC NULLS LAST
			""")
	Page<TravelEty> findFeedPageByUserIdAndFriendIds(
			@Param("userId") String userId,
			@Param("friendIds") List<String> friendIds,
			Pageable pageable
			);
	
	/**
	 * OTTIMIZZATO: Query CTE per feed - Modello Asimmetrico Puro + Mock Users (Opzionale)
	 * Usa Common Table Expression per materializzare gli amici una sola volta
	 * Performance: 90% più veloce rispetto a IN condition con lista dinamica
	 * 
	 * LOGICA MODELLO ASIMMETRICO (stile Instagram):
	 * - Mostra SOLO i viaggi di chi TU segui (esiste record userId→utente ACCEPTED)
	 * - Mostra i TUOI viaggi
	 * - Mostra SEMPRE i viaggi degli utenti MOCK se includeMockUsers=true (configurabile)
	 * 
	 * NON mostra viaggi di chi ti segue ma che tu non segui.
	 * 
	 * @param userId ID dell'utente corrente
	 * @param includeMockUsers Se true, include automaticamente tutti gli utenti mock (id LIKE 'mock-user-%')
	 * @param pageable Parametri di paginazione
	 */
	@Query(value = """
			WITH friend_ids AS (
			    -- Caso 1: Chi TU segui (mostra sempre i loro viaggi)
			    SELECT receiver_id as friend_id 
			    FROM friend_relationships 
			    WHERE requester_id = :userId 
			      AND status = 'ACCEPTED'
			    UNION
			    -- Caso 2: I tuoi viaggi (sempre)
			    SELECT :userId as friend_id
			    UNION
			    -- Caso 3: TUTTI gli utenti mock (solo se abilitato via config)
			    SELECT id as friend_id
			    FROM users
			    WHERE id LIKE 'mock-user-%'
			      AND :includeMockUsers = true
			)
			SELECT t.* 
			FROM travel t
			INNER JOIN users u ON t.user_id = u.id
			INNER JOIN friend_ids f ON t.user_id = f.friend_id
			ORDER BY t.date_to DESC NULLS LAST
			""", 
			countQuery = """
			WITH friend_ids AS (
			    SELECT receiver_id as friend_id 
			    FROM friend_relationships 
			    WHERE requester_id = :userId 
			      AND status = 'ACCEPTED'
			    UNION
			    SELECT :userId as friend_id
			    UNION
			    SELECT id as friend_id
			    FROM users
			    WHERE id LIKE 'mock-user-%'
			      AND :includeMockUsers = true
			)
			SELECT COUNT(t.id)
			FROM travel t
			INNER JOIN friend_ids f ON t.user_id = f.friend_id
			""",
			nativeQuery = true)
	Page<TravelEty> findFeedPageOptimized(
			@Param("userId") String userId,
			@Param("includeMockUsers") boolean includeMockUsers,
			Pageable pageable
	);
	
	/**
	 * OTTIMIZZATO: Eager loading per itinerari - STEP 1
	 * Evita N+1 queries e lazy loading durante la conversione DTO
	 * Da chiamare dopo findFeedPageOptimized per caricare i dettagli dei viaggi
	 * Nota: Diviso in 4 query per evitare MultipleBagFetchException
	 */
	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.itinerary
			WHERE t.id IN :travelIds
			ORDER BY t.dateTo DESC NULLS LAST
			""")
	List<TravelEty> fetchTravelDetailsForFeed(@Param("travelIds") List<Long> travelIds);
	
	/**
	 * OTTIMIZZATO: Eager loading per points - STEP 2
	 * Carica i punti degli itinerari separatamente
	 * Non può essere combinato con itinerary nella stessa query (MultipleBagFetchException)
	 */
	@Query("""
			SELECT DISTINCT i FROM DailyItineraryEty i
			LEFT JOIN FETCH i.points
			WHERE i.travel.id IN :travelIds
			""")
	List<DailyItineraryEty> fetchItineraryPointsForFeed(@Param("travelIds") List<Long> travelIds);
	
	/**
	 * OTTIMIZZATO: Eager loading per participants - STEP 3
	 * Carica i partecipanti separatamente per evitare MultipleBagFetchException
	 * Non può essere combinato con itinerary+points nella stessa query
	 */
	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.participants
			WHERE t.id IN :travelIds
			""")
	List<TravelEty> fetchTravelParticipantsForFeed(@Param("travelIds") List<Long> travelIds);
	
	/**
	 * OTTIMIZZATO: Eager loading per files - STEP 4
	 * Da chiamare dopo fetchTravelDetailsForFeed, fetchItineraryPointsForFeed e fetchTravelParticipantsForFeed
	 * Separato per evitare MultipleBagFetchException con multiple bag collections
	 */
	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			LEFT JOIN FETCH t.files
			WHERE t.id IN :travelIds
			""")
	List<TravelEty> fetchTravelFilesForFeed(@Param("travelIds") List<Long> travelIds);
	
	// =========================================================================
	// DEBUG QUERIES - Per diagnosticare problemi del feed
	// =========================================================================
	
	/**
	 * DEBUG: Query per verificare quali amici vengono trovati dalla CTE del feed
	 * Restituisce: [friend_id, source, status]
	 * Rispecchia la logica della query principale del feed (modello asimmetrico puro)
	 */
	@Query(value = """
			WITH friend_ids AS (
			    -- Caso 1: Chi TU segui (mostra sempre i loro viaggi)
			    SELECT receiver_id as friend_id, 
			           'TU_SEGUI' as source,
			           status
			    FROM friend_relationships 
			    WHERE requester_id = :userId 
			      AND status = 'ACCEPTED'
			    UNION
			    -- Caso 2: Te stesso
			    SELECT :userId as friend_id,
			           'SELF' as source,
			           'ACCEPTED' as status
			)
			SELECT friend_id, source, status
			FROM friend_ids
			""", nativeQuery = true)
	List<Object[]> debugFriendIds(@Param("userId") String userId);
	
	/**
	 * DEBUG: Query per verificare TUTTE le amicizie
	 * Restituisce: [requester_id, receiver_id, status]
	 */
	@Query(value = """
			SELECT requester_id, receiver_id, status
			FROM friend_relationships 
			WHERE (requester_id = :userId OR receiver_id = :userId)
			  AND status = 'ACCEPTED'
			ORDER BY created_at DESC
			""", nativeQuery = true)
	List<Object[]> debugAllFriendships(@Param("userId") String userId);
}
