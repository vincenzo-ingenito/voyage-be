package it.voyage.ms.service;

import java.util.List;
import java.util.Map;

import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.repository.entity.VoteType;

public interface ITravelVoteService {
    
    /**
     * Registra o aggiorna un voto per un viaggio
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente che vota
     * @param voteType Tipo di voto (UPVOTE/DOWNVOTE)
     * @return Statistiche aggiornate del viaggio
     */
    VoteStatsDTO vote(Long travelId, String userId, VoteType voteType);
    
    /**
     * Rimuove il voto di un utente per un viaggio
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente
     * @return Statistiche aggiornate del viaggio
     */
    VoteStatsDTO removeVote(Long travelId, String userId);
    
    /**
     * Ottiene le statistiche di voto per un viaggio
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente (per sapere se ha votato)
     * @return Statistiche di voto
     */
    VoteStatsDTO getVoteStats(Long travelId, String userId);
    
    /**
     * Ottiene le statistiche di voto per una lista di viaggi in batch (ottimizzato)
     * 
     * @param travelIds Lista degli ID dei viaggi
     * @param userId ID dell'utente (per sapere quali ha votato)
     * @return Mappa travelId -> VoteStatsDTO
     */
    Map<Long, VoteStatsDTO> getVoteStatsBatch(List<Long> travelIds, String userId);
}
