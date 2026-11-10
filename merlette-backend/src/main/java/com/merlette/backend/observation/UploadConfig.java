package com.merlette.backend.observation;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class UploadConfig implements WebMvcConfigurer {
    private final PhotoStorage storage;
    public UploadConfig(PhotoStorage storage) { this.storage = storage; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations(storage.directory().toUri().toString());
    }
}
