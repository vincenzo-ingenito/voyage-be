package it.voyage.ms.service;

/**
 * Servizio per l'invio di notifiche push Firebase Cloud Messaging
 */
public interface INotificationService {
    
    /**
     * Invia una notifica di invito a un viaggio di gruppo
     * @param userId ID dell'utente destinatario
     * @param inviterName Nome di chi ha invitato
     * @param travelName Nome del viaggio
     * @param travelId ID del viaggio
     */
    void sendGroupTravelInviteNotification(
        String userId, 
        String inviterName, 
        String travelName,
        Long travelId
    );
    
    /**
     * Invia una notifica quando qualcuno accetta un invito
     * @param ownerId ID del proprietario del viaggio
     * @param accepterName Nome di chi ha accettato
     * @param travelName Nome del viaggio
     */
    void sendInviteAcceptedNotification(
        String ownerId,
        String accepterName,
        String travelName
    );
}