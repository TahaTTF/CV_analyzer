package com.cvanalyzer.backend.enrichment;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.model.JobDocument;

@Service
public class LocalJobEnrichmentService implements LocalJobEnrichmentPort {
 public static final String VERSION="job-enrichment-1.0";
 private final DescriptionCleaningService cleaner;
 private final TechnicalSkillExtractor skillExtractor;
 private final LocationNormalizationService locationNormalizer;
 private final Clock clock;

 @Autowired
 public LocalJobEnrichmentService(DescriptionCleaningService cleaner,
   TechnicalSkillExtractor skillExtractor,LocationNormalizationService locationNormalizer){
  this(cleaner,skillExtractor,locationNormalizer,Clock.systemUTC());
 }
 LocalJobEnrichmentService(DescriptionCleaningService cleaner,
   TechnicalSkillExtractor skillExtractor,LocationNormalizationService locationNormalizer,Clock clock){
  this.cleaner=cleaner;this.skillExtractor=skillExtractor;
  this.locationNormalizer=locationNormalizer;this.clock=clock;
 }
 @Override public JobDocument enrich(JobDocument job){
  if(job==null)throw new IllegalArgumentException("job is required");
  if(job.enrichment()!=null&&VERSION.equals(job.enrichment().version()))return job;
  String descriptionText=cleaner.clean(job.description());
  var skills=skillExtractor.extract(job,descriptionText);
  var normalizedLocation=locationNormalizer.normalize(job,descriptionText);
  var enrichment=new JobDocument.JobEnrichment(descriptionText,skills.required(),
   skills.toBeAcquired(),normalizedLocation,VERSION,clock.instant());
  return job.withEnrichment(enrichment,clock.instant());
 }
}
