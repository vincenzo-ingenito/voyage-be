package it.voyage.ms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.controller.ITravelCtl;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.response.DeleteUserResponse;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelService;

@RestController
@RequestMapping("/api/travels")
public class TravelCtl implements ITravelCtl {

	@Autowired
	private ITravelService travelService;

	@Override
	public ResponseEntity<TravelDTO> saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
		TravelDTO savedEntity = travelService.saveTravel(travelData, files, userDetails);
		return ResponseEntity.ok( savedEntity); 
	}
	
	@Override
	public ResponseEntity<DeleteUserResponse> deleteTravelById(String travelId, CustomUserDetails userDetails) {
		Boolean deleted = travelService.deleteTravelById(travelId, userDetails.getUserId()); 
		if (deleted) {
			return ResponseEntity.ok(new DeleteUserResponse(true, "Viaggio eliminato con successo."));
		} else {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DeleteUserResponse(false, "Viaggio non trovato o non autorizzato all'eliminazione."));
		}
	}
	
	@Override
	public ResponseEntity<List<TravelDTO>> getTravels(CustomUserDetails userDetails) {
		List<TravelDTO> travels = travelService.getTravelsForUser(userDetails.getUserId());
		return ResponseEntity.ok(travels);
	}
	
	@PutMapping("/{travelId}") //TODO 
	public ResponseEntity<TravelDTO> updateTravel(String travelId, TravelDTO travelDTO, @AuthenticationPrincipal FirebaseToken userFirebase) {
		TravelDTO updatedTravel = travelService.updateExistingTravel(userFirebase.getUid(), travelId, travelDTO);
		return ResponseEntity.ok(updatedTravel); 
	}
}