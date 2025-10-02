package it.voyage.ms.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;

@Service
public class TravelService {

	@Autowired
	private TravelRepository travelRepository;
	
	 public TravelDTO updateExistingTravel(String ownerUid, String travelId, TravelDTO newTravelData){

	        Optional<TravelEty> existingTravelOpt = travelRepository.findByIdAndUserId(travelId, ownerUid);
 

	        TravelEty existingTravel = existingTravelOpt.get();

	        // 2. Aggiorna i campi dell'entità con i nuovi dati dal DTO
	        // Poiché il frontend invia l'intera struttura, aggiorniamo tutto:
	        
	        existingTravel.setTravelName(newTravelData.getTravelName());
	        existingTravel.setDateFrom(newTravelData.getDateFrom());
	        existingTravel.setDateTo(newTravelData.getDateTo());
	        
	        // Mappiamo l'itinerario DTO in Entità (necessita di una funzione di mappatura)
	        List<DailyItineraryDTO> updatedItinerary = mapDayDTOListToDayList(newTravelData.getItinerary());
	        existingTravel.setItinerary(updatedItinerary);
	        
	        // 3. Salva l'entità aggiornata nel database
	        TravelEty savedTravel = travelRepository.save(existingTravel);

	        // 4. Mappa l'entità salvata in DTO per la risposta al frontend
	        return TravelDTO.convertToDTO(savedTravel);
	    }
	 
	 protected List<DailyItineraryDTO> mapDayDTOListToDayList(List<DailyItineraryDTO> itineraryDTOs) {
	        if (itineraryDTOs == null) {
	            return List.of();
	        }
	        
	        return itineraryDTOs.stream()
	                .map(dayDTO -> {
	                    // Questa logica è quasi identica a quella che hai in convertToDocument
	                    DailyItineraryDTO dayEty = new DailyItineraryDTO(); 
	                    dayEty.setDay(dayDTO.getDay());
	                    dayEty.setDate(dayDTO.getDate());

	                    List<PointDTO> pointEties = dayDTO.getPoints().stream()
	                            .map(pointDTO -> {
	                                PointDTO pointEty = new PointDTO();
	                                pointEty.setName(pointDTO.getName());
	                                pointEty.setType(pointDTO.getType());
	                                pointEty.setDescription(pointDTO.getDescription());
	                                pointEty.setCost(pointDTO.getCost());
	                                pointEty.setCoord(new CoordsDto(pointDTO.getCoord().getLat(), pointDTO.getCoord().getLng()));
	                                pointEty.setCountry(pointDTO.getCountry());
	                                pointEty.setRegion(pointDTO.getRegion());
	                                pointEty.setCity(pointDTO.getCity());
	                                return pointEty;
	                            }).collect(Collectors.toList());

	                    dayEty.setPoints(pointEties);
	                    return dayEty;
	                }).collect(Collectors.toList());
	    }
}
