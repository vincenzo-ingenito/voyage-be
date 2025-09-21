package it.voyage.ms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults;
import it.voyage.ms.filter.AuthFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
	    return http
	            .csrf(AbstractHttpConfigurer::disable)
	            .cors(withDefaults()) 
	            .authorizeHttpRequests(auth -> auth
	                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
	                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
	                    .requestMatchers("/api/**").authenticated()
	                    .anyRequest().permitAll()
	            )
	            .addFilterBefore(new AuthFilter(), UsernamePasswordAuthenticationFilter.class)
	            .build();
	}
}
