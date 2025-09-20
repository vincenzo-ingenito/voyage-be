package it.voyage.ms.dto.response;

import java.util.Date;

import it.voyage.ms.repository.entity.UserEty;
import lombok.Data;

@Data
public class UserDto {

	private String name;
	private String email;
	private String avatar;
	private Date lastLogin;
	
    public static UserDto fromEntity(UserEty user) {
    	UserDto dto = new UserDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setLastLogin(user.getLastLogin());
        return dto;
    }
 
}
