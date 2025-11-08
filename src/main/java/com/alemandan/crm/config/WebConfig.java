package com.alemandan.crm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Configuración para exponer la carpeta uploads/ como recurso estático.
 * La ruta se toma desde la propiedad 'uploads.path' (por defecto 'uploads').
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${uploads.path:uploads}")
    private String uploadsPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convertir la ruta configurada a URI absoluta (file:/...)
        String absoluteUri = Paths.get(uploadsPath).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteUri);
    }
}