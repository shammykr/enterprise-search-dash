package com.shammy.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final SearchProperties properties;

    public WebConfig(SearchProperties properties) {
        this.properties = properties;
    }

    /** Only needed when the Angular dev server calls the API directly instead of through the proxy. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(properties.corsAllowedOrigins().split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE");
    }
}
