package com.cvanalyzer.backend.ingestion.adzuna;

import java.util.List;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.config.AdzunaProperties;
import com.cvanalyzer.backend.ingestion.IngestionSummary;
import com.cvanalyzer.backend.ingestion.JobPersistenceService;

@Service
public class AdzunaIngestionService {
 private final AdzunaClient client;private final AdzunaJobMapper mapper;
 private final JobPersistenceService persistence;private final AdzunaProperties properties;
 public AdzunaIngestionService(AdzunaClient client,AdzunaJobMapper mapper,
   JobPersistenceService persistence,AdzunaProperties properties){
  this.client=client;this.mapper=mapper;this.persistence=persistence;this.properties=properties;
 }
 public IngestionSummary ingest(){
  IngestionSummary total=new IngestionSummary(0,0,0,0,0);
  int pages=Math.min(properties.maxPages(),properties.requestBudget());
  for(int page=1;page<=pages;page++){
   AdzunaResponse response=client.fetchPage(page);
   List<AdzunaResponse.AdzunaJob> jobs=response.results()==null?List.of():response.results();
   total=add(total,persistence.persist("adzuna",jobs,mapper::map));
   if(jobs.size()<properties.resultsPerPage())break;
  }
  return total;
 }
 private IngestionSummary add(IngestionSummary a,IngestionSummary b){
  return new IngestionSummary(a.fetched()+b.fetched(),a.inserted()+b.inserted(),
   a.updated()+b.updated(),a.skipped()+b.skipped(),a.failed()+b.failed());
 }
}
