package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.service.IUserService;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthCtl {

	@Autowired
	private IUserService userService;
 
	@PostMapping("/login")
	public ResponseEntity<UserDto> login(@AuthenticationPrincipal FirebaseToken firebaseToken) {
		log.info("Login called");
		UserDto userDto = userService.login(firebaseToken);
		return new ResponseEntity<>(userDto, HttpStatus.OK);
	}
 

}