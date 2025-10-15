package it.voyage.ms.service;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.security.user.CustomUserDetails;

public interface IUserService {

	UserDto syncUserWithFirebase(CustomUserDetails customUserDetail);
	
	UserDto update(String firebaseId,UserDto userDto);
	
	boolean getPrivacyStatus(String firebaseId);
	
}
