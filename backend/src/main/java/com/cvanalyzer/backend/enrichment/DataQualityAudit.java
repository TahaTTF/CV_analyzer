package com.cvanalyzer.backend.enrichment;

import java.util.List;
import com.cvanalyzer.backend.model.JobSource;

public record DataQualityAudit(long total,List<SourceQuality> bySource,DuplicateSummary duplicates){
 public record SourceQuality(JobSource source,long total,long itOnly,long nonIt,long ambiguous,
  long unclassified,long missingDescriptions,long htmlDescriptions,long emptyTechnicalSkills,
  long missingCity,long missingCountry,long unknownWorkMode){}
 public record DuplicateSummary(long sameSourceExternalIdGroups,long identicalContentDifferentIdGroups,
  long repeatedTitleGroups,List<DuplicateExample> examples){}
 public record DuplicateExample(String kind,JobSource source,String title,List<String> externalIds){}
}
