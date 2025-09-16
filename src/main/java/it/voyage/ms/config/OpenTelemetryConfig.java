package it.voyage.ms.config;

import org.springframework.context.annotation.Bean;
import static it.voyage.ms.config.Constants.Properties.MS_NAME;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
@Configuration
public class OpenTelemetryConfig {

    @Bean
    public Tracer tracer() {
        return GlobalOpenTelemetry.getTracer(MS_NAME);
    }
}
