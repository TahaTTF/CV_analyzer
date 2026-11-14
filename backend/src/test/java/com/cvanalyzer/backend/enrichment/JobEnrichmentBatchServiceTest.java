package com.cvanalyzer.backend.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.repository.JobRepository;

class JobEnrichmentBatchServiceTest {
 @Test void dryRunReportsChangesWithZeroDatabaseWrites(){
  JobRepository repository=mock(JobRepository.class);LocalJobEnrichmentPort port=mock(LocalJobEnrichmentPort.class);
  JobDocument original=job(null);JobDocument enriched=job(enrichment("job-enrichment-1.0"));
  when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(original)));
  when(port.enrich(original)).thenReturn(enriched);
  EnrichmentSummary result=new JobEnrichmentBatchService(repository,port).run(true,100);
  assertThat(result.examined()).isOne();assertThat(result.wouldUpdate()).isOne();
  assertThat(result.updated()).isZero();verify(repository,never()).saveAll(any());
 }
 @Test void currentVersionCanRemainUnchanged(){
  JobRepository repository=mock(JobRepository.class);LocalJobEnrichmentPort port=mock(LocalJobEnrichmentPort.class);
  JobDocument current=job(enrichment(LocalJobEnrichmentService.VERSION));
  when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(current)));
  when(port.enrich(current)).thenReturn(current);
  EnrichmentSummary result=new JobEnrichmentBatchService(repository,port).run(true,100);
  assertThat(result.unchanged()).isOne();assertThat(result.wouldUpdate()).isZero();
 }
 private JobDocument.JobEnrichment enrichment(String version){return new JobDocument.JobEnrichment(
  "Java",List.of(new JobDocument.ExtractedSkill("Java",List.of("description"))),List.of(),
  new JobDocument.NormalizedLocation("Paris","Paris",null,"FR",JobDocument.WorkMode.UNKNOWN,1.0),
  version,Instant.parse("2026-10-21T10:00:00Z"));}
 private JobDocument job(JobDocument.JobEnrichment enrichment){
  Instant now=Instant.parse("2026-10-20T10:00:00Z");
  return new JobDocument("id",JobSource.ARBEITNOW,"ext","Java Developer",new JobDocument.Company("Acme"),
   "Java",new JobDocument.Location("Paris","Paris","FR",false),new JobDocument.Requirements(List.of()),
   List.of(),"https://example.test/job",now,now,now,JobStatus.ACTIVE,null,null,enrichment);
 }
}
