package it.voyage.ms.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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
     * Converte TravelEty in TravelDTO.
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
        
        // Nuovi campi per viaggi di gruppo
        dto.setTravelType(travel.getTravelType());
        dto.setOwnerId(travel.getUser() != null ? travel.getUser().getId() : null);
        // I partecipanti vengono popolati separatamente dal GroupTravelService quando necessario
        dto.setParticipants(new ArrayList<>());

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
     * Converte DailyItineraryEty in DailyItineraryDTO.
     */
    private DailyItineraryDTO convertDailyItineraryEtyToDTO(DailyItineraryEty dailyEty) {
        DailyItineraryDTO dto = new DailyItineraryDTO();
        dto.setDay(dailyEty.getDay());
        dto.setDate(dailyEty.getDate());
        dto.setMemoryImageIndex(dailyEty.getMemoryImageIndex());
        dto.setMemoryImageUrl(dailyEty.getMemoryImageUrl());

        if (dailyEty.getPoints() != null && !dailyEty.getPoints().isEmpty()) {
            List<PointDTO> pointDTOs = dailyEty.getPoints().stream()
                .sorted(Comparator.comparing(
                    PointEty::getOrderIndex,
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::convertPointEtyToDTO)
                .collect(Collectors.toList());
            dto.setPoints(pointDTOs);
        } else {
            dto.setPoints(Collections.emptyList());
        }

        return dto;
    }

    /**
     * Converte PointEty in PointDTO.
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

        if (pointEty.getLatitude() != null && pointEty.getLongitude() != null) {
            dto.setCoord(new CoordsDto(pointEty.getLatitude(), pointEty.getLongitude()));
        }

        // attachmentIndices è già List<Integer> — nessuna deserializzazione necessaria
        List<Integer> indices = pointEty.getAttachmentIndices();
        dto.setAttachmentIndices(indices != null ? indices : Collections.emptyList());
        dto.setAttachmentUrls(buildAttachmentUrls(pointEty));

        return dto;
    }

    /**
     * Costruisce la lista di AttachmentUrlDTO per un punto,
     * risalendo al Travel tramite DailyItinerary.
     */
    private List<AttachmentUrlDTO> buildAttachmentUrls(PointEty pointEty) {
        List<Integer> indices = pointEty.getAttachmentIndices();
        if (indices == null || indices.isEmpty()) {
            return Collections.emptyList();
        }

        TravelEty travel = pointEty.getDailyItinerary().getTravel();
        List<TravelFileEty> files = travel.getFiles();
        List<AttachmentUrlDTO> urls = new ArrayList<>();

        for (Integer index : indices) {
            if (index != null && index >= 0 && index < files.size()) {
                TravelFileEty file = files.get(index);
                AttachmentUrlDTO urlDto = new AttachmentUrlDTO();
                urlDto.setFileName(file.getFileName());
                urlDto.setMimeType(file.getMimeType());
                urlDto.setUrl(buildFileUrl(file.getFileId()));
                urls.add(urlDto);
            }
        }

        return urls;
    }

    /**
     * Costruisce l'URL del file a partire dal fileId.
     * Personalizza in base al tuo storage (Firebase, S3, ecc.).
     */
    private String buildFileUrl(String fileId) {
        return "/api/files/" + fileId;
    }

    /**
     * Converte TravelDTO in TravelEty.
     */
    public TravelEty convertDtoToEty(TravelDTO dto) {
        if (dto == null) {
            return null;
        }

        TravelEty travel = new TravelEty();

        if (dto.getTravelId() != null) {
            travel.setId(dto.getTravelId());
        }

        travel.setTravelName(dto.getTravelName());
        travel.setDateFrom(dto.getDateFrom());
        travel.setDateTo(dto.getDateTo());
        travel.setIsCopied(dto.getIsCopied());
        travel.setNeedsDateConfirmation(dto.getNeedsDateConfirmation());
        
        // Nuovo campo per viaggi di gruppo
        if (dto.getTravelType() != null) {
            travel.setTravelType(dto.getTravelType());
        }
        // I partecipanti vengono gestiti separatamente dal GroupTravelService
        // L'owner viene impostato dal TravelService quando crea/aggiorna il viaggio

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
     * Converte DailyItineraryDTO in DailyItineraryEty.
     */
    private DailyItineraryEty convertDailyItineraryDTOToEty(DailyItineraryDTO dto, TravelEty travel) {
        DailyItineraryEty dailyEty = new DailyItineraryEty();
        dailyEty.setDay(dto.getDay());
        dailyEty.setDate(dto.getDate().toString());
        dailyEty.setMemoryImageIndex(dto.getMemoryImageIndex());
        dailyEty.setMemoryImageUrl(dto.getMemoryImageUrl());
        dailyEty.setTravel(travel);

        if (dto.getPoints() != null && !dto.getPoints().isEmpty()) {
            List<PointEty> pointEties = new ArrayList<>();
            for (int i = 0; i < dto.getPoints().size(); i++) {
                pointEties.add(convertPointDTOToEty(dto.getPoints().get(i), dailyEty, i));
            }
            dailyEty.setPoints(pointEties);
        } else {
            dailyEty.setPoints(new ArrayList<>());
        }

        return dailyEty;
    }

    /**
     * Converte PointDTO in PointEty.
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
        pointEty.setOrderIndex(orderIndex);
        pointEty.setDailyItinerary(dailyItinerary);

        if (dto.getCoord() != null) {
            pointEty.setLatitude(dto.getCoord().getLat());
            pointEty.setLongitude(dto.getCoord().getLng());
        }

        // attachmentIndices è già List<Integer> — nessuna serializzazione necessaria
        List<Integer> indices = dto.getAttachmentIndices();
        pointEty.setAttachmentIndices(indices != null ? indices : new ArrayList<>());

        return pointEty;
    }
}