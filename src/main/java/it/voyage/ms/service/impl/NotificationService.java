package it.voyage.ms.service.impl;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import it.voyage.ms.enums.NotificationEnum;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final UserRepository userRepository;
    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendGroupTravelInviteNotification(String userId, String inviterName, String travelName, Long travelId) {
        NotificationEnum n = NotificationEnum.GROUP_TRAVEL_INVITE;
        Message.Builder message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(n.getTitle())
                        .setBody(String.format(n.getDescription(), inviterName, travelName))
                        .build())
                .putData("type", n.getType())
                .putData("travelId", travelId.toString())
                .putData("inviterName", inviterName)
                .putData("travelName", travelName);

        sendNotification(userId, message);
    }

    @Override
    public void sendInviteAcceptedNotification(String ownerId, String accepterName, String travelName) {
        NotificationEnum n = NotificationEnum.INVITE_ACCEPTED;
        Message.Builder message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(n.getTitle())
                        .setBody(String.format(n.getDescription(), accepterName, travelName))
                        .build())
                .putData("type", n.getType())
                .putData("accepterName", accepterName)
                .putData("travelName", travelName);

        sendNotification(ownerId, message);
    }

    @Override
    public void sendTravelLikeNotification(String travelOwnerId, String likerName, String travelName, Long travelId) {
        NotificationEnum n = NotificationEnum.SEND_LIKE;
        Message.Builder message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(n.getTitle())
                        .setBody(String.format(n.getDescription(), likerName, travelName))
                        .build())
                .putData("type", n.getType())
                .putData("likerName", likerName)
                .putData("travelName", travelName)
                .putData("travelId", travelId.toString());

        sendNotification(travelOwnerId, message);
    }

    @Override
    public void sendNewFollowerNotification(String followedUserId, String followerUsername, String followerId, String followerAvatar) {
        NotificationEnum n = NotificationEnum.NEW_FOLLOWER;
        Message.Builder message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(n.getTitle())
                        .setBody(String.format(n.getDescription(), followerUsername))
                        .build())
                .putData("type", n.getType())
                .putData("followerUsername", followerUsername)
                .putData("followerId", followerId)
                .putData("followerAvatar", followerAvatar != null ? followerAvatar : "");

        sendNotification(followedUserId, message);
    }

    @Override
    public void sendFriendRequestNotification(String receiverId, String requesterName, String requesterId, String requesterAvatar) {
        NotificationEnum n = NotificationEnum.FRIEND_REQUEST;
        Message.Builder message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(n.getTitle())
                        .setBody(String.format(n.getDescription(), requesterName))
                        .build())
                .putData("type", n.getType())
                .putData("requesterName", requesterName)
                .putData("requesterId", requesterId)
                .putData("requesterAvatar", requesterAvatar != null ? requesterAvatar : "");

        sendNotification(receiverId, message);
    }

    private void sendNotification(String receiverId, Message.Builder builder) {
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
            builder.setToken(fcmToken);
            firebaseMessaging.send(builder.build());
            log.info("Notifica FCM inviata con successo a utente: {}", receiverId);
        } catch (FirebaseMessagingException e) {
            log.error("Errore invio notifica FCM a utente {}: {}", receiverId, e.getMessage());
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED, INVALID_ARGUMENT -> {
                        log.warn("Token FCM non valido per utente {}, rimozione...", receiverId);
                        receiver.setFcmToken(null);
                        userRepository.save(receiver);
                    }
                    default -> log.error("Errore FCM non gestito: {}", e.getMessagingErrorCode());
                }
            }
        }
    }
}
