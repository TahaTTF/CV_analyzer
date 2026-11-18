package com.cvanalyzer.backend.ingestion.adzuna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.config.AdzunaProperties;
import com.cvanalyzer.backend.ingestion.*;

class AdzunaIngestionServiceTest {
 @Test void requestBudgetCapsConfiguredPages(){
  AdzunaClient client=mock(AdzunaClient.class);
  AdzunaJobMapper mapper=mock(AdzunaJobMapper.class);
  JobPersistenceService persistence=mock(JobPersistenceService.class);
  var props=new AdzunaProperties(URI.create("https://example.test"),"id","key","fr","",
   1,5,2,Duration.ofSeconds(1),Duration.ofSeconds(1));
  var response=new AdzunaResponse(10L,List.of(mock(AdzunaResponse.AdzunaJob.class)));
  when(client.fetchPage(anyInt())).thenReturn(response);
  when(persistence.persist(eq("adzuna"),anyList(),any())).thenReturn(new IngestionSummary(1,1,0,0,0));
  var result=new AdzunaIngestionService(client,mapper,persistence,props).ingest();
  assertThat(result.fetched()).isEqualTo(2);
  verify(client).fetchPage(1);verify(client).fetchPage(2);verifyNoMoreInteractions(client);
 }
}
