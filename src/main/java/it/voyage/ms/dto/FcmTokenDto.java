package it.voyage.ms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per registrazione/aggiornamento token FCM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenDto {
    
    @NotBlank(message = "Token FCM obbligatorio")
    @Size(min = 10, max = 500, message = "Token FCM non valido")
    private String token;
}