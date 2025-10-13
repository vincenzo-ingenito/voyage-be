package it.voyage.ms.config;


import java.util.Collections;
import java.util.regex.Pattern;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@OpenAPIDefinition(
	info = @Info(
			extensions = {
				@Extension(properties = {
					@ExtensionProperty(name = "x-api-id", value = "1"),
					@ExtensionProperty(name = "x-summary", value = "Voyage")
				})
			},
			title = "Voyage", 
			version = "1.0.0", 
			description = "Voyage",
			termsOfService = "${docs.info.termsOfService}", 
			contact = @Contact(name = "${docs.info.contact.name}", url = "${docs.info.contact.url}", email = "${docs.info.contact.mail}")))
public class OpenApiConfig {


  public OpenApiConfig() {
  }

  @Bean
	public OpenApiCustomizer openApiCustomiser() {
		return openApi -> openApi.getComponents().getSchemas().values().forEach( s -> s.setAdditionalProperties(false));
	}
	
	@Bean
	public OpenApiCustomizer customerGlobalHeaderOpenApiCustomiser() {
		return openApi -> {
			for (final Server server : openApi.getServers()) {
                final Pattern pattern = Pattern.compile("^https://.*");
                if (!pattern.matcher(server.getUrl()).matches()) {
                    server.addExtension("x-sandbox", true);
                }
            }
		};
	}
	
	 @Bean
	    public MappingJackson2HttpMessageConverter octetStreamJsonConverter() {
	        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
	        converter.setSupportedMediaTypes(
	                Collections.singletonList(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM));
	        return converter;
	    }
}

