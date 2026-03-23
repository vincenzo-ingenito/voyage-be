package it.voyage.ms.service.impl;

import org.springframework.stereotype.Service;

import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del servizio di notifiche
 * 
 * NOTA: Per abilitare le notifiche push, è necessario:
 * 1. Aggiungere la dipendenza firebase-admin nel pom.xml
 * 2. Salvare i token FCM degli utenti nel database (campo fcmToken in UserEty)
 * 3. Implementare l'invio effettivo tramite Firebase Cloud Messaging
 * 
 * Per ora questo servizio logga solo le notifiche che dovrebbero essere inviate.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService implements INotificationService {

    private final UserRepository userRepository;

    @Override
    public void sendGroupTravelInviteNotification(
            String userId,
            String inviterName,
            String travelName,
            Long travelId) {
        
        log.info("📧 NOTIFICA: Invito viaggio di gruppo");
        log.info("   Destinatario: {}", userId);
        log.info("   Da: {}", inviterName);
        log.info("   Viaggio: {}", travelName);
        log.info("   ID Viaggio: {}", travelId);
        
        UserEty user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Utente {} non trovato per notifica", userId);
            return;
        }
        
        // TODO: Implementare invio FCM quando disponibile
        // String fcmToken = user.getFcmToken();
        // if (fcmToken != null && !fcmToken.isEmpty()) {
        //     try {
        //         Message message = Message.builder()
        //             .setToken(fcmToken)
        //             .setNotification(Notification.builder()
        //                 .setTitle("Nuovo invito viaggio!")
        //                 .setBody(inviterName + " ti ha invitato a " + travelName)
        //                 .build())
        //             .putData("travelId", travelId.toString())
        //             .putData("type", "GROUP_TRAVEL_INVITE")
        //             .build();
        //         
        //         String response = FirebaseMessaging.getInstance().send(message);
        //         log.info("✅ Notifica FCM inviata: {}", response);
        //     } catch (Exception e) {
        //         log.error("❌ Errore invio notifica FCM", e);
        //     }
        // }
        
        log.info("⚠️ NOTIFICA SIMULATA (FCM non configurato)");
        log.info("   Titolo: Nuovo invito viaggio!");
        log.info("   Messaggio: {} ti ha invitato a {}", inviterName, travelName);
    }

    @Override
    public void sendInviteAcceptedNotification(
            String ownerId,
            String accepterName,
            String travelName) {
        
        log.info("📧 NOTIFICA: Invito accettato");
        log.info("   Destinatario: {}", ownerId);
        log.info("   Chi ha accettato: {}", accepterName);
        log.info("   Viaggio: {}", travelName);
        
        UserEty owner = userRepository.findById(ownerId).orElse(null);
        if (owner == null) {
            log.warn("Proprietario {} non trovato per notifica", ownerId);
            return;
        }
        
        // TODO: Implementare invio FCM quando disponibile
        log.info("⚠️ NOTIFICA SIMULATA (FCM non configurato)");
        log.info("   Titolo: Invito accettato!");
        log.info("   Messaggio: {} ha accettato l'invito a {}", accepterName, travelName);
    }
}