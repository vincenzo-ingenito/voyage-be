package it.voyage.ms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	 @Override
	    public void addCorsMappings(CorsRegistry registry) {
	        registry.addMapping("/**") // Applica a tutte le rotte API
	                .allowedOrigins("*") // Permette le richieste da qualsiasi origine
	                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permetti i metodi necessari
	                .allowedHeaders("*"); // Permetti tutti gli header
	    }
}
