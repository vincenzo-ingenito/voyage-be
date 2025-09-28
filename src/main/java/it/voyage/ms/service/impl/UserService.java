package it.voyage.ms.service.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.IUserService;

@Service
public class UserService implements IUserService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDto login(FirebaseToken firebaseToken) {
		Optional<UserEty> existingUser = userRepository.findById(firebaseToken.getUid());
		UserEty user;

		if (existingUser.isPresent()) {
			user = existingUser.get();
			user.setLastLogin(new Date());
		} else {
			user = new UserEty();
			user.setId(firebaseToken.getUid());
			user.setName(firebaseToken.getName());
			user.setEmail(firebaseToken.getEmail());
			user.setAvatar(firebaseToken.getPicture());
			user.setCreatedAt(new Date());
			user.setLastLogin(new Date());
			user.setPrivate(true);
			user.setBio("Nessuna biografia disponibile");
		}

		UserEty savedUser = userRepository.save(user);
		return UserDto.fromEntity(savedUser);
	}


}
