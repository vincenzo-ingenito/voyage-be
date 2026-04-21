package it.voyage.ms.dto.request;

import it.voyage.ms.repository.entity.VoteType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per le richieste di voto
 */
@Data
@NoArgsConstructor
public class VoteRequest {
    private VoteType voteType; // UPVOTE o DOWNVOTE, null per rimuovere il voto
}