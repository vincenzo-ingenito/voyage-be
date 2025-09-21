package it.voyage.ms.controller.impl;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.IUserService;

@RestController
@RequestMapping("/api/auth")
public class AuthCtl {

	@Autowired
	private IUserService userService;
	
	@Autowired
	private UserRepository userRepository;

	@PostMapping("/login")
	public ResponseEntity<UserDto> login(@AuthenticationPrincipal FirebaseToken firebaseToken) {
		UserDto userDto = userService.login(firebaseToken);
		return new ResponseEntity<>(userDto, HttpStatus.OK);
	}
	
	 @GetMapping("/privacy-status")
	    public ResponseEntity<?> getPrivacyStatus(@AuthenticationPrincipal FirebaseToken firebaseToken) {
	        String firebaseId = firebaseToken.getUid();

	        Optional<UserEty> user = userRepository.findById(firebaseId);
	        return new ResponseEntity<>(Map.of("isPrivate", user.get().isPrivate()), HttpStatus.OK);
	    }

    @PutMapping("/privacy")
    public ResponseEntity<?> updatePrivacyStatus(@AuthenticationPrincipal FirebaseToken firebaseToken,
                                                @RequestBody Map<String, Boolean> payload) {
        Boolean isPrivate = payload.get("isPrivate");
        if (isPrivate == null) {
            return new ResponseEntity<>("Invalid request payload", HttpStatus.BAD_REQUEST);
        }
        
        String firebaseId = firebaseToken.getUid();
        return userRepository.findById(firebaseId)
            .map(user -> {
                user.setPrivate(isPrivate);
                userRepository.save(user);
                return new ResponseEntity<>("Privacy status updated successfully", HttpStatus.OK);
            })
            .orElse(new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND));
    }
}