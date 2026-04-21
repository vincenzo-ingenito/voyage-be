package it.voyage.ms.dto.response;

import it.voyage.ms.repository.entity.VoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le statistiche di voto di un viaggio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteStatsDTO {
    private Long upvotes;      // Numero di upvotes
    private Long downvotes;    // Numero di downvotes
    private Long netScore;     // Punteggio netto (upvotes - downvotes)
    private VoteType userVote; // Voto dell'utente corrente (null se non ha votato)
}