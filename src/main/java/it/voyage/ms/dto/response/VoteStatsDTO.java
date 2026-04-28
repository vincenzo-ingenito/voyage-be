package it.voyage.ms.dto.response;

import it.voyage.ms.repository.entity.VoteType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le statistiche di like di un viaggio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteStatsDTO {
    private Long likes;        // Numero di like (rinominato da upvotes)
    private VoteType userVote; // Voto dell'utente corrente (null se non ha messo like)
    // downvotes e netScore rimossi - ora supportiamo solo i like
}
