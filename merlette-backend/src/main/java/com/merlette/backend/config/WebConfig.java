package com.merlette.backend.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Sert les images uploadées sur /uploads/<fichier>. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadDir;

    public WebConfig(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
