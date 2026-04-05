package it.voyage.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuitcaseDTO {

    private Long id;
    private String name;
    private String userId;
    private Long travelId;
    private String travelName;
    
    @Builder.Default
    private List<SuitcaseItemDTO> items = new ArrayList<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}