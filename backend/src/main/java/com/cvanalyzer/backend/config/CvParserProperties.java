package com.cvanalyzer.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix="app.cv")
public record CvParserProperties(
 @Min(1024) @Max(10_485_760) long maxFileBytes,
 @Min(10_000) @Max(2_000_000) int maxExtractedCharacters,
 @Min(1) @Max(500) int maxPdfPages) {}
