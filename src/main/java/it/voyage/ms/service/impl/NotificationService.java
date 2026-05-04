package it.voyage.ms.service.impl;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del servizio di notifiche FCM
 * Gestisce l'invio di notifiche push tramite Firebase Cloud Messaging
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendGroupTravelInviteNotification(String userId,
            String inviterName,
            String travelName,
            Long travelId) {
        
        log.info("Invio notifica invito viaggio di gruppo a utente: {}", userId);
        
        UserEty user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Utente {} non trovato per notifica", userId);
            return;
        }
        
        String fcmToken = user.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Utente {} non ha token FCM registrato", userId);
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("Nuovo invito viaggio!")
                    .setBody(inviterName + " ti ha invitato a " + travelName)
                    .build())
                .putData("travelId", travelId.toString())
                .putData("type", "GROUP_TRAVEL_INVITE")
                .putData("inviterName", inviterName)
                .putData("travelName", travelName)
                .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notifica FCM inviata con successo: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica FCM a utente {}: {}", userId, e.getMessage());
            
            // Se il token non è più valido, rimuovilo dal database
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("Token FCM non valido per utente {}, rimozione...", userId);
                        user.setFcmToken(null);
                        userRepository.save(user);
                        break;
                    default:
                        log.error("Errore FCM: {}", e.getMessagingErrorCode());
                }
            }
        } catch (Exception e) {
            log.error("Errore generico invio notifica", e);
        }
    }

    @Override
    public void sendInviteAcceptedNotification(String ownerId, String accepterName, String travelName) {
        
        log.info("Invio notifica invito accettato a utente: {}", ownerId);
        
        UserEty owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            log.warn("Proprietario {} non trovato per notifica", ownerId);
            return;
        }
        
        String fcmToken = owner.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Utente {} non ha token FCM registrato", ownerId);
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("Invito accettato!")
                    .setBody(accepterName + " ha accettato l'invito a " + travelName)
                    .build())
                .putData("type", "INVITE_ACCEPTED")
                .putData("accepterName", accepterName)
                .putData("travelName", travelName)
                .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notifica FCM inviata con successo: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica FCM a utente {}: {}", ownerId, e.getMessage());
            
            // Se il token non è più valido, rimuovilo dal database
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("🗑️ Token FCM non valido per utente {}, rimozione...", ownerId);
                        owner.setFcmToken(null);
                        userRepository.save(owner);
                        break;
                    default:
                        log.error("Errore FCM: {}", e.getMessagingErrorCode());
                }
            }
        } catch (Exception e) {
            log.error("Errore generico invio notifica", e);
        }
    }

    @Override
    public void sendTravelLikeNotification(String travelOwnerId, String likerName, String travelName, Long travelId) {
        
        log.info("📬 Invio notifica like a proprietario viaggio: {}", travelOwnerId);
        
        UserEty owner = userRepository.findById(travelOwnerId).orElse(null);
        if (owner == null) {
            log.warn("Proprietario {} non trovato per notifica", travelOwnerId);
            return;
        }
        
        String fcmToken = owner.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Utente {} non ha token FCM registrato", travelOwnerId);
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("Nuovo like!")
                    .setBody(likerName + " ha messo mi piace a " + travelName)
                    .build())
                .putData("type", "LIKE")
                .putData("likerName", likerName)
                .putData("travelName", travelName)
                .putData("travelId", travelId.toString())
                .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notifica like inviata con successo: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica like a utente {}: {}", travelOwnerId, e.getMessage());
            
            // Se il token non è più valido, rimuovilo dal database
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("Token FCM non valido per utente {}, rimozione...", travelOwnerId);
                        owner.setFcmToken(null);
                        userRepository.save(owner);
                        break;
                    default:
                        log.error("Errore FCM: {}", e.getMessagingErrorCode());
                }
            }
        } catch (Exception e) {
            log.error("Errore generico invio notifica like", e);
        }
    }

    @Override
    public void sendNewFollowerNotification(String followedUserId, String followerUsername, String followerId, String followerAvatar) {
        
        log.info("Invio notifica nuovo follower a utente: {}", followedUserId);
        
        UserEty followedUser = userRepository.findById(followedUserId).orElse(null);
        if (followedUser == null) {
            log.warn("Utente seguito {} non trovato per notifica", followedUserId);
            return;
        }
        
        String fcmToken = followedUser.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Utente {} non ha token FCM registrato", followedUserId);
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("Nuovo follower!")
                    .setBody(followerUsername + " ha iniziato a seguirti")
                    .build())
                .putData("type", "NEW_FOLLOWER")
                .putData("followerUsername", followerUsername)
                .putData("followerId", followerId)
                .putData("followerAvatar", followerAvatar != null ? followerAvatar : "")
                .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notifica nuovo follower inviata con successo: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica follower a utente {}: {}", followedUserId, e.getMessage());
            
            // Se il token non è più valido, rimuovilo dal database
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("Token FCM non valido per utente {}, rimozione...", followedUserId);
                        followedUser.setFcmToken(null);
                        userRepository.save(followedUser);
                        break;
                    default:
                        log.error("Errore FCM: {}", e.getMessagingErrorCode());
                }
            }
        } catch (Exception e) {
            log.error("Errore generico invio notifica follower", e);
        }
    }

    @Override
    public void sendFriendRequestNotification(String receiverId, String requesterName, String requesterId, String requesterAvatar) {
        
        log.info("Invio notifica richiesta amicizia a utente: {}", receiverId);
        
        UserEty receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null) {
            log.warn("Destinatario {} non trovato per notifica", receiverId);
            return;
        }
        
        String fcmToken = receiver.getFcmToken();
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Utente {} non ha token FCM registrato", receiverId);
            return;
        }
        
        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle("Nuova richiesta di amicizia!")
                    .setBody(requesterName + " ti ha inviato una richiesta di amicizia")
                    .build())
                .putData("type", "FRIEND_REQUEST")
                .putData("requesterName", requesterName)
                .putData("requesterId", requesterId)
                .putData("requesterAvatar", requesterAvatar != null ? requesterAvatar : "")
                .build();
            
            String response = firebaseMessaging.send(message);
            log.info("Notifica richiesta amicizia inviata con successo: {}", response);
            
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica richiesta amicizia a utente {}: {}", receiverId, e.getMessage());
            
            // Se il token non è più valido, rimuovilo dal database
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("Token FCM non valido per utente {}, rimozione...", receiverId);
                        receiver.setFcmToken(null);
                        userRepository.save(receiver);
                        break;
                    default:
                        log.error("Errore FCM: {}", e.getMessagingErrorCode());
                }
            }
        } catch (Exception e) {
            log.error("Errore generico invio notifica richiesta amicizia", e);
        }
    }
}
