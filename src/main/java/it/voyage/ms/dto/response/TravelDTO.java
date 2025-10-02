package it.voyage.ms.dto.response;

import java.util.List;
import java.util.stream.Collectors;

import it.voyage.ms.repository.entity.TravelEty;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TravelDTO {
	private String travelId;
	private String travelName; 
	private List<DailyItineraryDTO> itinerary;
	private String dateFrom; 
	private String dateTo;   

	public static TravelDTO convertToDTO(TravelEty travel) {
		TravelDTO dto = new TravelDTO();
		dto.setTravelId(travel.getId());
		dto.setTravelName(travel.getTravelName());
		dto.setDateFrom(travel.getDateFrom());
		dto.setDateTo(travel.getDateTo());

		List<DailyItineraryDTO> dayDTOs = travel.getItinerary().stream()
				.map(day -> {
					DailyItineraryDTO dayDTO = new DailyItineraryDTO();
					dayDTO.setDay(day.getDay());
					dayDTO.setDate(day.getDate());

					List<PointDTO> pointDTOs = day.getPoints().stream()
							.map(point -> {
								PointDTO pointDTO = new PointDTO();
								pointDTO.setName(point.getName());
								pointDTO.setType(point.getType());
								pointDTO.setDescription(point.getDescription());
								pointDTO.setCost(point.getCost());
								CoordsDto coords = new CoordsDto(point.getCoord().getLat(),point.getCoord().getLng());
								pointDTO.setCoord(coords);
								pointDTO.setCountry(point.getCountry());
								pointDTO.setRegion(point.getRegion());
								pointDTO.setCity(point.getCity());
								return pointDTO;
							}).collect(Collectors.toList());

					dayDTO.setPoints(pointDTOs);
					return dayDTO;
				}).collect(Collectors.toList());

		dto.setItinerary(dayDTOs);
		return dto;
	}
}