package it.voyage.ms.repository.impl;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.UserEty;

@Repository
public interface UserRepository extends JpaRepository<UserEty, String> {
    
    /**
     * Esegue una ricerca flessibile per nome utente utilizzando ILIKE
     * case-insensitive.
     * @param query La stringa di ricerca.
     * @return Una lista di utenti che corrispondono alla query.
     */
    @Query("SELECT u FROM UserEty u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<UserEty> findByNameRegex(@Param("query") String query);
}