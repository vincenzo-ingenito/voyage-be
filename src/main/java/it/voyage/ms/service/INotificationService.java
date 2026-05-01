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
    
    /**
     * Invia una notifica quando qualcuno mette like a un viaggio
     * @param travelOwnerId ID del proprietario del viaggio
     * @param likerName Nome di chi ha messo like
     * @param travelName Nome del viaggio
     * @param travelId ID del viaggio
     */
    void sendTravelLikeNotification(
        String travelOwnerId,
        String likerName,
        String travelName,
        Long travelId
    );
    
    /**
     * Invia una notifica quando un utente inizia a seguire un altro utente
     * @param followedUserId ID dell'utente seguito
     * @param followerUsername Username del follower
     * @param followerId ID del follower
     * @param followerAvatar Avatar del follower (opzionale)
     */
    void sendNewFollowerNotification(
        String followedUserId,
        String followerUsername,
        String followerId,
        String followerAvatar
    );
    
    /**
     * Invia una notifica quando qualcuno invia una richiesta di amicizia
     * @param receiverId ID del destinatario della richiesta
     * @param requesterName Nome di chi ha inviato la richiesta
     * @param requesterId ID di chi ha inviato la richiesta
     * @param requesterAvatar Avatar di chi ha inviato la richiesta
     */
    void sendFriendRequestNotification(
        String receiverId,
        String requesterName,
        String requesterId,
        String requesterAvatar
    );
}
