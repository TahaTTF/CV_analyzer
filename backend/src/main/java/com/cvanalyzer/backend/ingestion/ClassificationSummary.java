package com.cvanalyzer.backend.ingestion;

public record ClassificationSummary(long examined,long classified,long relevant,long nonIt,long ambiguous) {}
