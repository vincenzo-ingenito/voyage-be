package it.voyage.ms.dto.response;

import java.util.Date;

import it.voyage.ms.repository.entity.UserEty;
import lombok.Data;

@Data
public class UserDto {

	private String id;
	private String name;
	private String email;
	private String avatar;
	private Date lastLogin;
	private boolean isCurrentUser;
	private String bio;
	private Boolean privateProfile;
	private Boolean showEmergencyFAB;
	private Boolean isAiUser;

	public static UserDto fromEntity(UserEty user) {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setName(user.getName());
		dto.setEmail(user.getEmail());
		dto.setAvatar(user.getAvatar());
		dto.setLastLogin(user.getLastLogin());
		dto.setCurrentUser(false);
		dto.setBio(user.getBio());
		dto.setPrivateProfile(user.isPrivate());
		dto.setShowEmergencyFAB(user.isShowEmergencyFAB());
		dto.setIsAiUser(user.isAiUser());
		return dto;
	}

	public static UserDto fromEntityWithUid(UserEty user, String loggedInUid) {
		UserDto dto = new UserDto();
		dto.setId(user.getId());
		dto.setName(user.getName());
		dto.setEmail(user.getEmail());
		dto.setAvatar(user.getAvatar());
		dto.setLastLogin(user.getLastLogin());
		dto.setCurrentUser(user.getId().equals(loggedInUid));
		dto.setBio(user.getBio());
		dto.setPrivateProfile(user.isPrivate());
		dto.setShowEmergencyFAB(user.isShowEmergencyFAB());
		dto.setIsAiUser(user.isAiUser());
		return dto;
	}

}
