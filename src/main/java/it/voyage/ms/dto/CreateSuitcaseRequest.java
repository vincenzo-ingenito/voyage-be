package it.voyage.ms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSuitcaseRequest {

    private String name;
    private Long travelId; // Opzionale: può essere null se non associata subito a un viaggio
    
    @Builder.Default
    private List<SuitcaseItemDTO> items = new ArrayList<>();
}