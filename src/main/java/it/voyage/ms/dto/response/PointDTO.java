package it.voyage.ms.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PointDTO {
	private String name;
	private String type;
	private String description;
	private String cost;
	private CoordsDto coord;
	private String country; 
	private String region;  
	private String city;    
 
}