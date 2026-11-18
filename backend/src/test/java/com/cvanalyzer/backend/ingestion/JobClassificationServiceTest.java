package com.cvanalyzer.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.repository.JobRepository;

class JobClassificationServiceTest {
 @Test void onlyBackfillsUnclassifiedDocuments(){
  Clock clock=Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC);
  JobRepository repository=mock(JobRepository.class);
  JobDocument unclassified=job("one",null);
  JobDocument.ItClassification prior=new JobDocument.ItClassification(true,List.of("prior"),
   "already done","old",clock.instant());
  when(repository.findAll()).thenReturn(List.of(unclassified,job("two",prior)));
  var service=new JobClassificationService(repository,new ItJobClassifier(clock),clock);
  ClassificationSummary summary=service.classifyUnclassified();
  assertThat(summary.examined()).isEqualTo(2);
  assertThat(summary.classified()).isOne();
  ArgumentCaptor<List<JobDocument>> saved=ArgumentCaptor.forClass(List.class);
  verify(repository).saveAll(saved.capture());
  assertThat(saved.getValue()).singleElement().satisfies(job->{
   assertThat(job.externalId()).isEqualTo("one");
   assertThat(job.itClassification().isItRelevant()).isTrue();
  });
 }
 @Test void reclassifiesOnlyMissingOrOutdatedDocuments(){
  Clock clock=Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC);
  JobRepository repository=mock(JobRepository.class);
  JobDocument.ItClassification old=new JobDocument.ItClassification(false,List.of(),
   "old rules","it-rules-1.0",clock.instant());
  JobDocument.ItClassification current=new JobDocument.ItClassification(true,List.of(),
   "current rules",ItJobClassifier.VERSION,clock.instant());
  when(repository.findAll()).thenReturn(List.of(job("missing",null),job("old",old),job("current",current)));
  var service=new JobClassificationService(repository,new ItJobClassifier(clock),clock);

  ClassificationSummary summary=service.classifyOutdated();

  assertThat(summary.examined()).isEqualTo(3);
  assertThat(summary.classified()).isEqualTo(2);
  ArgumentCaptor<List<JobDocument>> saved=ArgumentCaptor.forClass(List.class);
  verify(repository).saveAll(saved.capture());
  assertThat(saved.getValue()).extracting(JobDocument::externalId).containsExactly("missing","old");
  assertThat(saved.getValue()).allSatisfy(job->
   assertThat(job.itClassification().version()).isEqualTo(ItJobClassifier.VERSION));
 }
 private JobDocument job(String externalId,JobDocument.ItClassification classification){
  Instant now=Instant.parse("2026-10-20T10:00:00Z");
  return new JobDocument(null,JobSource.ARBEITNOW,externalId,"Software Engineer",
   new JobDocument.Company("Acme"),"Java",new JobDocument.Location("Paris",null,"FR",false),
   new JobDocument.Requirements(List.of()),List.of(),"https://example.test/"+externalId,
   now,now,now,JobStatus.ACTIVE,null,classification);
 }
}
