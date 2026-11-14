package com.cvanalyzer.backend.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.ingestion.arbeitnow")
public record ArbeitnowProperties(@NotNull URI baseUrl, @Min(1) @Max(10) int maxPages,
        @Min(1) @Max(10) int requestBudget, @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {}
