package com.cvanalyzer.backend.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.model.*;

class LocalJobEnrichmentServiceTest {
 private final Clock clock=Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC);
 private final LocalJobEnrichmentService service=new LocalJobEnrichmentService(
  new DescriptionCleaningService(),new TechnicalSkillExtractor(),new LocationNormalizationService(),clock);

 @Test void enrichesWithoutChangingOriginalProviderData(){
  JobDocument original=job();JobDocument result=service.enrich(original);
  assertThat(result.id()).isEqualTo(original.id());
  assertThat(result.externalId()).isEqualTo(original.externalId());
  assertThat(result.description()).isEqualTo(original.description());
  assertThat(result.location()).isEqualTo(original.location());
  assertThat(result.requirements()).isEqualTo(original.requirements());
  assertThat(result.enrichment().descriptionText()).isEqualTo("Java & Docker");
  assertThat(result.enrichment().requiredSkills()).extracting(JobDocument.ExtractedSkill::name)
   .contains("Java","Docker");
 }
 @Test void isIdempotentForCurrentVersionAndReprocessesOldVersion(){
  JobDocument first=service.enrich(job());
  assertThat(service.enrich(first)).isSameAs(first);
  var old=new JobDocument.JobEnrichment("old",List.of(),List.of(),null,"job-enrichment-0.9",clock.instant());
  JobDocument reprocessed=service.enrich(job().withEnrichment(old,clock.instant()));
  assertThat(reprocessed.enrichment().version()).isEqualTo(LocalJobEnrichmentService.VERSION);
  assertThat(reprocessed.enrichment().descriptionText()).isEqualTo("Java & Docker");
 }
 private JobDocument job(){
  Instant now=Instant.parse("2026-10-20T10:00:00Z");
  return new JobDocument("id",JobSource.ARBEITNOW,"ext","Java Developer",
   new JobDocument.Company("Acme"),"<p>Java &amp; Docker</p>",
   new JobDocument.Location("Paris, France","Paris","FR",false),
   new JobDocument.Requirements(List.of("Java")),List.of("Full Time"),"https://example.test/job",
   now,now,now,JobStatus.ACTIVE,null,null);
 }
}
