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
public class SuitcaseItemDTO {

    private Long id;
    private String name;
    
    @Builder.Default
    private Boolean isChecked = false;
    
    private Integer quantity;
    private String category;
    private LocalDateTime createdAt;
}