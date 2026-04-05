package it.voyage.ms.repository;

import it.voyage.ms.repository.entity.SuitcaseItemEty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuitcaseItemRepository extends JpaRepository<SuitcaseItemEty, Long> {

    List<SuitcaseItemEty> findBySuitcaseId(Long suitcaseId);

    void deleteBySuitcaseId(Long suitcaseId);
}