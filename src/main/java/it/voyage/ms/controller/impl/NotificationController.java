package it.voyage.ms.controller.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.FcmTokenDto;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller per la gestione delle notifiche push FCM
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "API per la gestione delle notifiche push")
public class NotificationController {

    private final IUserService userService;

    @PostMapping("/fcm-token")
    @Operation(summary = "Registra o aggiorna token FCM", description = "Salva il token FCM del dispositivo per ricevere notifiche push")
    public ResponseEntity<Void> registerFcmToken(@Valid @RequestBody FcmTokenDto tokenDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        log.info("Registrazione token FCM per utente: {}", userId);
        userService.updateFcmToken(userId, tokenDto.getToken());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/fcm-token")
    @Operation(summary = "Rimuove token FCM", description = "Rimuove il token FCM al logout o disinstallazione app")
    public ResponseEntity<Void> removeFcmToken(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        log.info("Rimozione token FCM per utente: {}", userId);
        userService.removeFcmToken(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/complete-login")
    @Operation(summary = "Completa setup post-login", description = "Registra il token FCM dopo il login, risolvendo race conditions")
    public ResponseEntity<Void> completeLoginSetup(@Valid @RequestBody FcmTokenDto tokenDto, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        log.info("Completamento setup login per utente: {}", userId);
        userService.completeLoginSetup(userId, tokenDto.getToken());
        return ResponseEntity.ok().build();
    }
}
