package it.voyage.ms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.controller.ITravelCtl;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.response.DeleteUserResponse;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TravelCtl implements ITravelCtl {

	@Autowired
	private ITravelService travelService;

	@Override
	public ResponseEntity<TravelDTO> saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
		log.info("Called save travel ep");
		
		if (travelData == null) {
			log.error("Nessun dato viaggio fornito");
			return ResponseEntity.badRequest().build();
		}
		
		TravelDTO savedEntity = travelService.saveTravel(travelData, files, userDetails);
		return ResponseEntity.ok(savedEntity); 
	}
	
	@Override
	public ResponseEntity<DeleteUserResponse> deleteTravelById(String travelId, CustomUserDetails userDetails) {
		log.info("Called delete trave by id ep");
		Boolean deleted = travelService.deleteTravelById(travelId, userDetails.getUserId()); 
		if (deleted) {
			return ResponseEntity.ok(new DeleteUserResponse(true, "Viaggio eliminato con successo."));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DeleteUserResponse(false, "Viaggio non trovato o non autorizzato all'eliminazione."));
		}
	}
	
	@Override
	public ResponseEntity<List<TravelDTO>> getTravels(CustomUserDetails userDetails) {
		log.info("Called get travels ep");
		List<TravelDTO> travels = travelService.getTravelsForUser(userDetails.getUserId());
		return ResponseEntity.ok(travels);
	}
	
	@Override
	public ResponseEntity<TravelDTO> updateTravel(@PathVariable String travelId, @RequestPart("travelData") TravelDTO travelData, @RequestPart(value = "files", required = false) List<MultipartFile> files, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called update travel ep");
		TravelDTO updatedTravel = travelService.updateExistingTravel(userDetails.getUserId(), travelId, travelData, files);
		return ResponseEntity.ok(updatedTravel); 
	}
	
	@Override
	public ResponseEntity<TravelDTO> confirmTravelDates(@PathVariable String travelId, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called confirm travel dates ep for travelId: {}", travelId);
		TravelDTO confirmedTravel = travelService.confirmTravelDates(userDetails.getUserId(), travelId);
		return ResponseEntity.ok(confirmedTravel);
	}
}
