package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.controller.IUserCtl;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.response.PrivacyStatusResponse;
import it.voyage.ms.service.IUserService;

@RestController
public class UserCtl implements IUserCtl {

	@Autowired
	private IUserService userService;

	@Override
	public ResponseEntity<UserDto> updateUserDetails(UserDto updateDTO, FirebaseToken firebaseToken) {
		 String firebaseId = firebaseToken.getUid();
		 UserDto updatedUser = userService.update(firebaseId, updateDTO);
		 return new ResponseEntity<UserDto>(updatedUser,HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PrivacyStatusResponse> getPrivacyStatus(FirebaseToken firebaseToken) {
		String firebaseId = firebaseToken.getUid();

		boolean isPrivate = userService.getPrivacyStatus(firebaseId);
		return new ResponseEntity<>(new PrivacyStatusResponse(isPrivate), HttpStatus.OK);
	}
	
}
