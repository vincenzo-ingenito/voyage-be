package it.voyage.ms.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.security.user.CustomUserDetails;

public interface ITravelService {

	Boolean deleteTravelById(String travelId, String userId);
	
	TravelDTO updateExistingTravel(String ownerUid, String travelId, TravelDTO newTravelData);
	
	List<TravelDTO> getTravelsForUser(String userId);
	
	TravelDTO saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails);
	
//	List<CountryVisit> getUniqueConsolidatedCountryVisits(String userId);;
	
	List<CountryVisit> getConsolidatedCountryVisits(String userId);
}
