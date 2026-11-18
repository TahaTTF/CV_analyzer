package com.cvanalyzer.backend.ingestion;
public record IngestionSummary(int fetched,int inserted,int updated,int skipped,int failed) {}
