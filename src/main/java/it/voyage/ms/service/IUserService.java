package it.voyage.ms.service;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.UserDto;

public interface IUserService {

	UserDto login(FirebaseToken firebaseToken);
	
	UserDto update(String firebaseId,UserDto userDto);
	
	boolean getPrivacyStatus(String firebaseId);
	
}
