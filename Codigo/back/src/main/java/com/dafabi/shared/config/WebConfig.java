package com.dafabi.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String[] LOCAL_ORIGINS = {
            "http://localhost:3000",
            "http://localhost:4200",
            "http://localhost:4300",
            "http://localhost:5173",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:4300",
            "http://127.0.0.1:5173",
            "https://dafabi.gustavoarc.com.br",
            "https://dafabi-front.vercel.app",
            "https://dafabi.flxcloud.dev"
    };

    @Value("${app.cors.allowed-origin:}")
    private String configuredOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins())
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location")
                .allowCredentials(true);
    }

    private String[] allowedOrigins() {
        if (configuredOrigins == null || configuredOrigins.isBlank()) {
            return LOCAL_ORIGINS;
        }

        return Arrays.stream(
                        (configuredOrigins + "," + String.join(",", LOCAL_ORIGINS)).split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }
}
