package com.cvanalyzer.backend.enrichment;

public record EnrichmentSummary(boolean dryRun,long examined,long wouldUpdate,long updated,
 long unchanged,long failed,long jobsWithCleanedDescriptions,long jobsWithExtractedSkills,
 long jobsWithNormalizedLocations,String version) {}
