package it.voyage.ms.security.filter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.enums.RoleEnum;
import it.voyage.ms.security.user.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
	    
	    String authHeader = request.getHeader("Authorization");

	    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
	        filterChain.doFilter(request, response);
            return; 
	    }

        String idToken = authHeader.substring(7);

	    try {
	        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Collection<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(RoleEnum.ROLE_USER.name()));
            CustomUserDetails principal = new CustomUserDetails(decodedToken.getUid(), decodedToken.getEmail(), decodedToken.getName(), decodedToken.getPicture(), authorities);
	        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
	        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
	        SecurityContextHolder.getContext().setAuthentication(authentication);
	    } catch (FirebaseAuthException e) {
	        log.warn("Tentativo di accesso non autorizzato (Firebase): {}", e.getMessage());
	        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	        return;
	        
	    } catch (Exception e) {
	        log.error("Errore interno durante l'autenticazione: {}", e.getMessage(), e);
	        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
	    }

	    filterChain.doFilter(request, response);
	}
	
}
