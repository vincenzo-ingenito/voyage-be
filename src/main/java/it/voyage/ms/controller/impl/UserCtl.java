package it.voyage.ms.controller.impl;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IUserCtl;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.response.PrivacyStatusResponse;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IUserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class UserCtl implements IUserCtl {

	@Autowired
	private IUserService userService;

	@Override
	public ResponseEntity<UserDto> updateUserDetails(UserDto updateDTO, CustomUserDetails customerUserDetail) {
		log.info("Called update user detail ep");
		 UserDto updatedUser = userService.update(customerUserDetail.getUserId(), updateDTO);
		 return new ResponseEntity<UserDto>(updatedUser,HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PrivacyStatusResponse> getPrivacyStatus(CustomUserDetails customerUserDetail) {
		log.info("Called get privacy status ep");
		boolean isPrivate = userService.getPrivacyStatus(customerUserDetail.getUserId());
		return new ResponseEntity<>(new PrivacyStatusResponse(isPrivate), HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<Map<String, String>> deleteAccount(CustomUserDetails customerUserDetail) {
		log.info("🗑️ Called delete account endpoint for userId: {}", customerUserDetail.getUserId());
		
		try {
			boolean deleted = userService.deleteAccount(customerUserDetail.getUserId());
			
			if (deleted) {
				Map<String, String> response = new HashMap<>();
				response.put("message", "Account eliminato con successo");
				response.put("userId", customerUserDetail.getUserId());
				log.info("✅ Account eliminato con successo per userId: {}", customerUserDetail.getUserId());
				return new ResponseEntity<>(response, HttpStatus.OK);
			} else {
				Map<String, String> response = new HashMap<>();
				response.put("error", "Impossibile eliminare l'account");
				return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			
		} catch (Exception e) {
			log.error("❌ Errore durante l'eliminazione dell'account: {}", e.getMessage(), e);
			Map<String, String> response = new HashMap<>();
			response.put("error", "Errore durante l'eliminazione dell'account: " + e.getMessage());
			return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
}
