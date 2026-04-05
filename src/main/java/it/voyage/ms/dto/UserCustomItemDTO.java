package it.voyage.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomItemDTO {
    
    private Long id;
    private String name;
    private String category;
    private LocalDateTime createdAt;
}