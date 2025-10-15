package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IUserCtl;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.response.PrivacyStatusResponse;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IUserService;

@RestController
public class UserCtl implements IUserCtl {

	@Autowired
	private IUserService userService;

	@Override
	public ResponseEntity<UserDto> updateUserDetails(UserDto updateDTO, CustomUserDetails customerUserDetail) {
		 UserDto updatedUser = userService.update(customerUserDetail.getUserId(), updateDTO);
		 return new ResponseEntity<UserDto>(updatedUser,HttpStatus.OK);
	}

	@Override
	public ResponseEntity<PrivacyStatusResponse> getPrivacyStatus(CustomUserDetails customerUserDetail) {
		boolean isPrivate = userService.getPrivacyStatus(customerUserDetail.getUserId());
		return new ResponseEntity<>(new PrivacyStatusResponse(isPrivate), HttpStatus.OK);
	}
	
}
