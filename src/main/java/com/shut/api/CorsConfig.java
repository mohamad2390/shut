package com.shut.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the web app call the API from its own address. Set {@code app.cors.allowed-origins} to a comma separated list,
 * for example {@code http://localhost:5173}. Left empty, no other origin is allowed.
 */
@Configuration
class CorsConfig implements WebMvcConfigurer {

    private final String[] origins;

    CorsConfig(@Value("${app.cors.allowed-origins:}") String[] origins) {
        this.origins = java.util.Arrays.stream(origins).map(String::trim).filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origins.length > 0) {
            registry.addMapping("/api/**").allowedOrigins(origins).allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
        }
    }
}
