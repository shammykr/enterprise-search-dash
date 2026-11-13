package com.shammy.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public record SearchProperties(String indexName, boolean seedSampleData, String corsAllowedOrigins) {
}
