package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IAuthCtl;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IUserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class AuthCtl implements IAuthCtl {

	@Autowired
	private IUserService userService;
 
	@Override
	public ResponseEntity<UserDto> login(CustomUserDetails customUserDetail) {
		UserDto userDto = userService.syncUserWithFirebase(customUserDetail);
		return new ResponseEntity<>(userDto, HttpStatus.OK);
	}

}