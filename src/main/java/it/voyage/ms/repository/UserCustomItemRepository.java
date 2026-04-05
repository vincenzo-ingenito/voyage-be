package it.voyage.ms.repository;

import it.voyage.ms.repository.entity.UserCustomItemEty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomItemRepository extends JpaRepository<UserCustomItemEty, Long> {
    
    /**
     * Trova tutti gli oggetti personalizzati di un utente
     */
    List<UserCustomItemEty> findByUserId(String userId);
    
    /**
     * Trova un oggetto personalizzato per utente e nome
     */
    Optional<UserCustomItemEty> findByUserIdAndName(String userId, String name);
    
    /**
     * Verifica se esiste già un oggetto con lo stesso nome per l'utente
     */
    boolean existsByUserIdAndName(String userId, String name);
}