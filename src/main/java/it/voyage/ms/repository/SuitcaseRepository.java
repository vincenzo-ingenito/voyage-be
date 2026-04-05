package it.voyage.ms.repository;

import it.voyage.ms.repository.entity.SuitcaseEty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuitcaseRepository extends JpaRepository<SuitcaseEty, Long> {

    List<SuitcaseEty> findByUserId(String userId);

    List<SuitcaseEty> findByUserIdAndTravelIsNull(String userId);

    List<SuitcaseEty> findByUserIdAndTravelId(String userId, Long travelId);

    @Query("SELECT s FROM SuitcaseEty s LEFT JOIN FETCH s.items WHERE s.id = :id AND s.userId = :userId")
    Optional<SuitcaseEty> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") String userId);

    Optional<SuitcaseEty> findByIdAndUserId(Long id, String userId);
}
