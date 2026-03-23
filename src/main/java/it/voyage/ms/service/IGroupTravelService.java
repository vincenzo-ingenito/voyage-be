package it.voyage.ms.service;

import java.util.List;

import it.voyage.ms.dto.request.ParticipantInviteRequest;
import it.voyage.ms.dto.response.ParticipantDTO;
import it.voyage.ms.repository.entity.ParticipantRole;

/**
 * Interfaccia per la gestione dei viaggi di gruppo
 */
public interface IGroupTravelService {

    /**
     * Invita uno o più partecipanti a un viaggio di gruppo
     * 
     * @param travelId ID del viaggio
     * @param invites Lista di inviti da inviare
     * @param invitedBy ID dell'utente che invia gli inviti (owner)
     * @return Lista dei partecipanti invitati
     */
    List<ParticipantDTO> inviteParticipants(
        Long travelId,
        List<ParticipantInviteRequest> invites,
        String invitedBy
    );

    /**
     * Ottiene tutti i partecipanti di un viaggio
     * 
     * @param travelId ID del viaggio
     * @return Lista di tutti i partecipanti
     */
    List<ParticipantDTO> getParticipants(Long travelId);

    /**
     * Risponde a un invito (accetta o rifiuta)
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente che risponde
     * @param accept true per accettare, false per rifiutare
     * @return Il partecipante aggiornato
     */
    ParticipantDTO respondToInvite(Long travelId, String userId, boolean accept);

    /**
     * Cambia il ruolo di un partecipante (solo owner)
     * 
     * @param travelId ID del viaggio
     * @param userId ID del partecipante
     * @param newRole Nuovo ruolo da assegnare
     * @param requesterId ID dell'utente che richiede il cambio (deve essere owner)
     * @return Il partecipante con il ruolo aggiornato
     */
    ParticipantDTO updateParticipantRole(
        Long travelId,
        String userId,
        ParticipantRole newRole,
        String requesterId
    );

    /**
     * Rimuove un partecipante da un viaggio (solo owner)
     * 
     * @param travelId ID del viaggio
     * @param userId ID del partecipante da rimuovere
     * @param requesterId ID dell'utente che richiede la rimozione (deve essere owner)
     */
    void removeParticipant(Long travelId, String userId, String requesterId);

    /**
     * Ottiene tutti i viaggi di gruppo a cui un utente partecipa
     * 
     * @param userId ID dell'utente
     * @return Lista dei viaggi a cui partecipa
     */
    List<ParticipantDTO> getUserGroupTravelParticipations(String userId);

    /**
     * Verifica se un utente può modificare un viaggio
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente
     * @return true se può modificare (è owner o editor)
     */
    boolean canUserEditTravel(Long travelId, String userId);

    /**
     * Verifica se un utente può visualizzare un viaggio
     * 
     * @param travelId ID del viaggio
     * @param userId ID dell'utente
     * @return true se può visualizzare (è owner, partecipante o amico dell'owner)
     */
    boolean canUserViewTravel(Long travelId, String userId);
}
