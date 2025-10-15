package it.voyage.ms.security.user;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
public class CustomUserDetails implements UserDetails {
	
    private static final long serialVersionUID = -8577888476005165870L;
	private final String userId; 
    private final String email;  
    private final String fullName; 
    private final String avatarUrl;
    private final Collection<? extends GrantedAuthority> authorities;
    
	@Override
	public String getPassword() {
		return null;
	}
	@Override
	public String getUsername() {
		return userId;
	}

   
}