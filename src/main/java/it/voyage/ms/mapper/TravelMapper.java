package it.voyage.ms.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.voyage.ms.dto.response.AttachmentUrlDTO;
import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelFileEty;

@Component
public class TravelMapper {

	/**
	 * Converte TravelEty in TravelDTO
	 * Gestisce la conversione completa di itinerario, giorni e punti
	 */
	public TravelDTO convertEtyToDTO(TravelEty travel) {
	    if (travel == null) {
	        return null;
	    }
	    
	    TravelDTO dto = new TravelDTO();
	    dto.setTravelId(travel.getId());
	    dto.setTravelName(travel.getTravelName());
	    dto.setDateFrom(travel.getDateFrom());
	    dto.setDateTo(travel.getDateTo());
	    dto.setIsCopied(travel.getIsCopied());
	    dto.setNeedsDateConfirmation(travel.getNeedsDateConfirmation());

	    // Conversione itinerario con gestione null-safe
	    if (travel.getItinerary() != null && !travel.getItinerary().isEmpty()) {
	        List<DailyItineraryDTO> itineraryDTOs = travel.getItinerary().stream()
	            .map(this::convertDailyItineraryEtyToDTO)
	            .collect(Collectors.toList());
	        dto.setItinerary(itineraryDTOs);
	    } else {
	        dto.setItinerary(Collections.emptyList());
	    }

	    return dto;
	}

	/**
	 * Converte DailyItineraryEty in DailyItineraryDTO
	 */
	private DailyItineraryDTO convertDailyItineraryEtyToDTO(DailyItineraryEty dailyEty) {
	    DailyItineraryDTO dto = new DailyItineraryDTO();
	    dto.setDay(dailyEty.getDay());
	    dto.setDate(dailyEty.getDate());
	    dto.setMemoryImageIndex(dailyEty.getMemoryImageIndex());
	    dto.setMemoryImageUrl(dailyEty.getMemoryImageUrl());

	    // Conversione punti
	    if (dailyEty.getPoints() != null && !dailyEty.getPoints().isEmpty()) {
	        List<PointDTO> pointDTOs = dailyEty.getPoints().stream()
	            .sorted(Comparator.comparing(PointEty::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
	            .map(this::convertPointEtyToDTO)
	            .collect(Collectors.toList());
	        dto.setPoints(pointDTOs);
	    } else {
	        dto.setPoints(Collections.emptyList());
	    }

	    return dto;
	}

	/**
	 * Converte PointEty in PointDTO
	 */
	private PointDTO convertPointEtyToDTO(PointEty pointEty) {
	    PointDTO dto = new PointDTO();
	    dto.setName(pointEty.getName());
	    dto.setType(pointEty.getType());
	    dto.setDescription(pointEty.getDescription());
	    dto.setCost(pointEty.getCost());
	    dto.setCountry(pointEty.getCountry());
	    dto.setRegion(pointEty.getRegion());
	    dto.setCity(pointEty.getCity());

	    // Coordinate
	    if (pointEty.getLatitude() != null && pointEty.getLongitude() != null) {
	        CoordsDto coords = new CoordsDto(pointEty.getLatitude(),pointEty.getLongitude());
	        dto.setCoord(coords);
	    }

	    // Gestione attachment indices da JSON
	    if (pointEty.getAttachmentIndicesJson() != null && !pointEty.getAttachmentIndicesJson().isEmpty()) {
	        try {
	            ObjectMapper mapper = new ObjectMapper();
	            List<Integer> indices = mapper.readValue(
	                pointEty.getAttachmentIndicesJson(),
	                new TypeReference<List<Integer>>() {}
	            );
	            dto.setAttachmentIndices(indices);
	        } catch (JsonProcessingException e) {
	            dto.setAttachmentIndices(Collections.emptyList());
	        }
	    } else {
	        dto.setAttachmentIndices(Collections.emptyList());
	    }

	    // Recupera gli URL degli attachment dal Travel parent
	    // Assumendo che tu abbia accesso al TravelEty tramite DailyItinerary
	    dto.setAttachmentUrls(buildAttachmentUrls(pointEty));

	    return dto;
	}

	/**
	 * Costruisce la lista di AttachmentUrlDTO per un punto
	 */
	private List<AttachmentUrlDTO> buildAttachmentUrls(PointEty pointEty) {
	    List<AttachmentUrlDTO> urls = new ArrayList<>();
	    
	    if (pointEty.getAttachmentIndicesJson() == null || pointEty.getAttachmentIndicesJson().isEmpty()) {
	        return urls;
	    }

	    try {
	        ObjectMapper mapper = new ObjectMapper();
	        List<Integer> indices = mapper.readValue(
	            pointEty.getAttachmentIndicesJson(),
	            new TypeReference<List<Integer>>() {}
	        );

	        // Recupera il Travel attraverso DailyItinerary
	        TravelEty travel = pointEty.getDailyItinerary().getTravel();
	        List<TravelFileEty> files = travel.getFiles();

	        for (Integer index : indices) {
	            if (index >= 0 && index < files.size()) {
	                TravelFileEty file = files.get(index);
	                AttachmentUrlDTO urlDto = new AttachmentUrlDTO();
	                urlDto.setFileName(file.getFileName());
	                urlDto.setMimeType(file.getMimeType());
	                // Qui dovresti costruire l'URL effettivo basato sul tuo storage
	                urlDto.setUrl(buildFileUrl(file.getFileId()));
	                urls.add(urlDto);
	            }
	        }
	    } catch (JsonProcessingException e) {
	        // Log dell'errore
	    }

	    return urls;
	}

	/**
	 * Costruisce l'URL del file (personalizza in base al tuo storage)
	 */
	private String buildFileUrl(String fileId) {
	    // Esempio: se usi Firebase Storage, S3, etc.
	    return "/api/files/" + fileId;
	}

	/**
	 * Converte TravelDTO in TravelEty
	 * Crea automaticamente le entità relazionali per itinerario, daily itinerary e points
	 */
	public TravelEty convertDtoToEty(TravelDTO dto) {
	    if (dto == null) {
	        return null;
	    }

	    TravelEty travel = new TravelEty();

	    // Setta l'ID direttamente se presente
	    if (dto.getTravelId() != null) {
	        travel.setId(dto.getTravelId());
	    }

	    travel.setTravelName(dto.getTravelName());
	    travel.setDateFrom(dto.getDateFrom());
	    travel.setDateTo(dto.getDateTo());
	    travel.setIsCopied(dto.getIsCopied());
	    travel.setNeedsDateConfirmation(dto.getNeedsDateConfirmation());

	    // Conversione itinerario
	    if (dto.getItinerary() != null && !dto.getItinerary().isEmpty()) {
	        List<DailyItineraryEty> itineraryEties = dto.getItinerary().stream()
	            .map(dailyDTO -> convertDailyItineraryDTOToEty(dailyDTO, travel))
	            .collect(Collectors.toList());
	        travel.setItinerary(itineraryEties);
	    } else {
	        travel.setItinerary(new ArrayList<>());
	    }

	    return travel;
	}

	/**
	 * Converte DailyItineraryDTO in DailyItineraryEty
	 */
	private DailyItineraryEty convertDailyItineraryDTOToEty(DailyItineraryDTO dto, TravelEty travel) {
	    DailyItineraryEty dailyEty = new DailyItineraryEty();
	    dailyEty.setDay(dto.getDay());
	    dailyEty.setDate(dto.getDate());
	    dailyEty.setMemoryImageIndex(dto.getMemoryImageIndex());
	    dailyEty.setMemoryImageUrl(dto.getMemoryImageUrl());
	    dailyEty.setTravel(travel); // Importante: setta la relazione bidirezionale

	    // Conversione punti
	    if (dto.getPoints() != null && !dto.getPoints().isEmpty()) {
	        List<PointEty> pointEties = new ArrayList<>();
	        for (int i = 0; i < dto.getPoints().size(); i++) {
	            PointDTO pointDTO = dto.getPoints().get(i);
	            PointEty pointEty = convertPointDTOToEty(pointDTO, dailyEty, i);
	            pointEties.add(pointEty);
	        }
	        dailyEty.setPoints(pointEties);
	    } else {
	        dailyEty.setPoints(new ArrayList<>());
	    }

	    return dailyEty;
	}

	/**
	 * Converte PointDTO in PointEty
	 */
	private PointEty convertPointDTOToEty(PointDTO dto, DailyItineraryEty dailyItinerary, int orderIndex) {
	    PointEty pointEty = new PointEty();
	    pointEty.setName(dto.getName());
	    pointEty.setType(dto.getType());
	    pointEty.setDescription(dto.getDescription());
	    pointEty.setCost(dto.getCost());
	    pointEty.setCountry(dto.getCountry());
	    pointEty.setRegion(dto.getRegion());
	    pointEty.setCity(dto.getCity());
	    pointEty.setOrderIndex(orderIndex); // Setta l'ordine automaticamente
	    pointEty.setDailyItinerary(dailyItinerary); // Importante: setta la relazione bidirezionale

	    // Coordinate
	    if (dto.getCoord() != null) {
	        pointEty.setLatitude(dto.getCoord().getLat());
	        pointEty.setLongitude(dto.getCoord().getLng());
	    }

	    // Conversione attachment indices in JSON
	    if (dto.getAttachmentIndices() != null && !dto.getAttachmentIndices().isEmpty()) {
	        try {
	            ObjectMapper mapper = new ObjectMapper();
	            String json = mapper.writeValueAsString(dto.getAttachmentIndices());
	            pointEty.setAttachmentIndicesJson(json);
	        } catch (JsonProcessingException e) {
	            pointEty.setAttachmentIndicesJson("[]");
	        }
	    } else {
	        pointEty.setAttachmentIndicesJson("[]");
	    }

	    return pointEty;
	}
}