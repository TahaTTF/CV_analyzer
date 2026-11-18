package com.cvanalyzer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="app.ingestion")
public record IngestionProperties(boolean enabled) {}
