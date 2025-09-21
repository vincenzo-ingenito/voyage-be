package it.voyage.ms.controller.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;

@RestController
@RequestMapping("/api/travels")
public class TravelCtl {

	@Autowired
	private TravelRepository travelRepo;
	
	@GetMapping("")
	public ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal FirebaseToken userFirebase) {
		List<TravelEty> travelEty = travelRepo.findByUserId(userFirebase.getUid());
		List<TravelDTO> output = new ArrayList<>();
		for(TravelEty t : travelEty) {
			output.add(TravelDTO.convertToDTO(t));
		}
		return ResponseEntity.ok(output);
	}
	
	@DeleteMapping("/{travelId}")
	public ResponseEntity<String> deleteTravelById(@PathVariable String travelId, @AuthenticationPrincipal FirebaseToken userFirebase) {
	    long deletedCount = travelRepo.deleteByIdAndUserId(travelId, userFirebase.getUid());
	    return deletedCount > 0 ? ResponseEntity.ok("Viaggio eliminato con successo") : ResponseEntity.notFound().build();
	}


}
