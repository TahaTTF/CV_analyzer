package com.cvanalyzer.backend.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix="app.ingestion.adzuna")
public record AdzunaProperties(@NotNull URI baseUrl,String appId,String appKey,String country,
        String query,@Min(1) @Max(50) int resultsPerPage,@Min(1) @Max(25) int maxPages,
        @Min(1) @Max(25) int requestBudget,@NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {}
