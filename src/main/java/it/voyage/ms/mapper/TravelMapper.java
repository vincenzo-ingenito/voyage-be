package it.voyage.ms.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.TravelEty;

@Component
public class TravelMapper {

	public TravelDTO convertEtyToDTO(TravelEty travel) {
	    TravelDTO dto = new TravelDTO();
	    
	    dto.setTravelId(travel.getId()); 
	    dto.setTravelName(travel.getTravelName());
	    dto.setDateFrom(travel.getDateFrom());
	    dto.setDateTo(travel.getDateTo());
	    dto.setIsCopied(travel.getIsCopied());
	    dto.setNeedsDateConfirmation(travel.getNeedsDateConfirmation());

	    if (travel.getItinerary() != null) {
	        List<DailyItineraryDTO> dayDTOs = travel.getItinerary().stream()
	                .map(this::mapDailyItineraryEtyToDto)  
	                .collect(Collectors.toList());
	        
	        dto.setItinerary(dayDTOs);
	    } else {
	        dto.setItinerary(Collections.emptyList());
	    }
	    return dto;
	}
	
	// Mappa una singola PointEty in PointDTO
	private PointDTO mapPointEtyToDto(PointDTO point) { // Assumo PointEty e PointDTO usano la stessa classe per i campi base
	    PointDTO pointDTO = new PointDTO();
	    pointDTO.setName(point.getName());
	    pointDTO.setType(point.getType());
	    pointDTO.setDescription(point.getDescription());
	    pointDTO.setCost(point.getCost());
	    
	    if (point.getCoord() != null) {
	        CoordsDto coords = new CoordsDto(point.getCoord().getLat(), point.getCoord().getLng());
	        pointDTO.setCoord(coords);
	    }
	    
	    pointDTO.setCountry(point.getCountry());
	    pointDTO.setRegion(point.getRegion());
	    pointDTO.setCity(point.getCity());

	    // Mappa gli indici degli allegati (temporanei o permanenti, a seconda dello stato dell'Entity)
	    pointDTO.setAttachmentIndices(point.getAttachmentIndices());
	    
	    return pointDTO;
	}
	
	private DailyItineraryDTO mapDailyItineraryEtyToDto(DailyItineraryDTO day) {
	    DailyItineraryDTO dayDTO = new DailyItineraryDTO();
	    dayDTO.setDay(day.getDay());
	    dayDTO.setDate(day.getDate());

	    dayDTO.setMemoryImageIndex(day.getMemoryImageIndex());

	    if (day.getPoints() != null) {
	        List<PointDTO> pointDTOs = day.getPoints().stream()
	                .map(this::mapPointEtyToDto)  
	                .collect(Collectors.toList());
	        
	        dayDTO.setPoints(pointDTOs);
	    } else {
	        dayDTO.setPoints(Collections.emptyList());
	    }

	    return dayDTO;
	}
	
	public TravelEty convertDtoToEty(TravelDTO dto) {
	    TravelEty travel = new TravelEty();
	    
	    travel.setId(dto.getTravelId()); 
	    travel.setTravelName(dto.getTravelName());
	    travel.setDateFrom(dto.getDateFrom());
	    travel.setDateTo(dto.getDateTo());
	    travel.setIsCopied(dto.getIsCopied());
	    travel.setNeedsDateConfirmation(dto.getNeedsDateConfirmation());

	    if (dto.getItinerary() != null) {
	        List<DailyItineraryDTO> itineraryDocuments = dto.getItinerary().stream()
	                .map(this::mapDailyItineraryDto)
	                .collect(Collectors.toList());
	        
	        travel.setItinerary(itineraryDocuments);
	    } else {
	        travel.setItinerary(Collections.emptyList());
	    }

	    return travel;
	}
	
	private DailyItineraryDTO mapDailyItineraryDto(DailyItineraryDTO dayDTO) {
	    DailyItineraryDTO day = new DailyItineraryDTO();
	    day.setDay(dayDTO.getDay());
	    day.setDate(dayDTO.getDate());
	    day.setMemoryImageIndex(dayDTO.getMemoryImageIndex()); 

	    if (dayDTO.getPoints() != null) {
	        List<PointDTO> pointDocuments = dayDTO.getPoints().stream()
	                .map(this::mapPointDto)  
	                .collect(Collectors.toList());
	        
	        day.setPoints(pointDocuments);
	    } else {
	        day.setPoints(Collections.emptyList());
	    }
	    
	    return day;
	}
	
	private PointDTO mapPointDto(PointDTO pointDTO) {
	    PointDTO point = new PointDTO();
	    point.setName(pointDTO.getName());
	    point.setType(pointDTO.getType());
	    point.setDescription(pointDTO.getDescription());
	    point.setCost(pointDTO.getCost());
	    
	    if (pointDTO.getCoord() != null) {
	        point.setCoord(new CoordsDto(pointDTO.getCoord().getLat(), pointDTO.getCoord().getLng()));
	    }
	    
	    point.setCountry(pointDTO.getCountry());
	    point.setRegion(pointDTO.getRegion());
	    point.setCity(pointDTO.getCity());
	    point.setAttachmentIndices(pointDTO.getAttachmentIndices());
	    return point;
	}
}
