package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoordsDto {

	private Double lat;
    private Double lng;
}
