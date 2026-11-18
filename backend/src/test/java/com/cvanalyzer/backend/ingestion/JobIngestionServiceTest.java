package com.cvanalyzer.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.config.ArbeitnowProperties;
import com.cvanalyzer.backend.exception.ExternalServiceException;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowClient;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowJobMapper;
import com.cvanalyzer.backend.ingestion.arbeitnow.ArbeitnowResponse;
import com.cvanalyzer.backend.model.*;
import com.cvanalyzer.backend.repository.JobRepository;

class JobIngestionServiceTest {
 private ArbeitnowClient client;
 private ArbeitnowJobMapper mapper;
 private JobRepository repository;
 private JobIngestionService service;

 @BeforeEach void setUp(){
  client=mock(ArbeitnowClient.class); mapper=mock(ArbeitnowJobMapper.class);
  repository=mock(JobRepository.class);
  var props=new ArbeitnowProperties(URI.create("https://example.test"),1,1,
    Duration.ofSeconds(1),Duration.ofSeconds(1));
  service=new JobIngestionService(client,mapper,repository,props,
    Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"),ZoneOffset.UTC));
 }

 @Test void insertsNewJob(){
  arrange(job(null,"Engineer"));
  when(repository.findBySourceAndExternalId(JobSource.ARBEITNOW,"slug")).thenReturn(Optional.empty());
  assertThat(service.ingestArbeitnow()).isEqualTo(new IngestionSummary(1,1,0,0,0));
  verify(repository).save(any(JobDocument.class));
 }

 @Test void skipsUnchangedJob(){
  JobDocument current=job("id","Engineer"); arrange(job(null,"Engineer"));
  when(repository.findBySourceAndExternalId(JobSource.ARBEITNOW,"slug")).thenReturn(Optional.of(current));
  assertThat(service.ingestArbeitnow()).isEqualTo(new IngestionSummary(1,0,0,1,0));
  verify(repository,never()).save(any());
 }

 @Test void updatesChangedJobAndKeepsId(){
  JobDocument current=job("id","Old title"); arrange(job(null,"New title"));
  when(repository.findBySourceAndExternalId(JobSource.ARBEITNOW,"slug")).thenReturn(Optional.of(current));
  assertThat(service.ingestArbeitnow()).isEqualTo(new IngestionSummary(1,0,1,0,0));
  verify(repository).save(argThat(saved->saved.id().equals("id")&&saved.title().equals("New title")));
 }

 @Test void propagatesExternalApiFailure(){
  when(client.fetchPage(1)).thenThrow(new ExternalServiceException("failed",new RuntimeException()));
  assertThatThrownBy(()->service.ingestArbeitnow()).isInstanceOf(ExternalServiceException.class);
  verifyNoInteractions(mapper,repository);
 }

 private void arrange(JobDocument normalized){
  var source=new ArbeitnowResponse.ArbeitnowJob("slug","Example",normalized.title(),null,false,
    "https://example.test/job",List.of(),List.of(),"Berlin",1L);
  when(client.fetchPage(1)).thenReturn(new ArbeitnowResponse(List.of(source),null,null));
  when(mapper.map(source)).thenReturn(normalized);
 }
 private JobDocument job(String id,String title){
  Instant time=Instant.parse("2026-10-20T09:00:00Z");
  return new JobDocument(id,JobSource.ARBEITNOW,"slug",title,new JobDocument.Company("Example"),
    "Description",new JobDocument.Location("Berlin",null,null,false),
    new JobDocument.Requirements(List.of()),List.of(),"https://example.test/job",
    Instant.ofEpochSecond(1),time,time,JobStatus.ACTIVE);
 }
}
