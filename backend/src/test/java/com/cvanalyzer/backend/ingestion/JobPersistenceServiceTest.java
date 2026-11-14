package com.cvanalyzer.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.repository.JobRepository;

class JobPersistenceServiceTest {
 @Test void insertsThenSkipsBySourceAndExternalId(){
  JobRepository repository=mock(JobRepository.class);
  JobDocument job=job();
  var service=new JobPersistenceService(repository,Clock.fixed(
   Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC));
  when(repository.findBySourceAndExternalId(JobSource.ADZUNA,"42"))
   .thenReturn(Optional.empty(),Optional.of(job));
  assertThat(service.persist("adzuna",List.of(job),value->value).inserted()).isOne();
  assertThat(service.persist("adzuna",List.of(job),value->value).skipped()).isOne();
  verify(repository,times(1)).save(job);
 }
 private JobDocument job(){
  Instant now=Instant.parse("2026-10-21T10:00:00Z");
  return new JobDocument("id",JobSource.ADZUNA,"42","Software Engineer",
   new JobDocument.Company("Acme"),"Description",
   new JobDocument.Location("Paris",null,"FR",false),new JobDocument.Requirements(List.of()),
   List.of(),"https://example.test/job",now,now,now,JobStatus.ACTIVE);
 }
}
