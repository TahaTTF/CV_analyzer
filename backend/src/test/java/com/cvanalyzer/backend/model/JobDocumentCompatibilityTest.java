package com.cvanalyzer.backend.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobDocumentCompatibilityTest {
 @Test void legacyConstructorLeavesNewMetadataOptional(){
  Instant now=Instant.parse("2026-10-21T10:00:00Z");
  JobDocument legacy=new JobDocument("id",JobSource.ARBEITNOW,"slug","Engineer",
   new JobDocument.Company("Acme"),"Description",
   new JobDocument.Location("Berlin",null,null,false),new JobDocument.Requirements(List.of()),
   List.of(),"https://example.test/job",now,now,now,JobStatus.ACTIVE);
  assertThat(legacy.sourceMetadata()).isNull();
  assertThat(legacy.itClassification()).isNull();
  assertThat(legacy.enrichment()).isNull();
  assertThat(legacy.requirements().toBeAcquiredSkills()).isEmpty();
 }
}
