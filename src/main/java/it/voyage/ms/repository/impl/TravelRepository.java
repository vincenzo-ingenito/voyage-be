package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
			WHERE t.user.id = :userId
			OR t.user.id IN :friendIds
			ORDER BY t.dateTo DESC NULLS LAST
			""")
	Page<TravelEty> findFeedPageByUserIdAndFriendIds(
			@Param("userId") String userId,
			@Param("friendIds") List<String> friendIds,
			Pageable pageable
			);
}
