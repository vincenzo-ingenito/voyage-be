package it.voyage.ms.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.FeedPageDTO;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.security.user.CustomUserDetails;

public interface ITravelService {

	Boolean deleteTravelById(Long travelId, String userId);
	
	TravelDTO updateExistingTravel(String ownerUid, Long travelId, TravelDTO newTravelData, List<MultipartFile> files);
	
	List<TravelDTO> getTravelsForUser(String userId);
	
	TravelDTO getTravelWithUrls(String userId, Long travelId);
	
	TravelDTO saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails);
	
	TravelDTO confirmTravelDates(String userId, String travelId);
	
	List<CountryVisit> getConsolidatedCountryVisits(String userId);
	
	/**
	 * Recupera il feed paginato dei viaggi (propri + amici)
	 * con cursor-based pagination per ottimizzare performance e memoria
	 */
	FeedPageDTO getFeedPaginated(String userId, int pageSize, String cursor, boolean includePhotos);
}
