package com.cvanalyzer.backend.ingestion.labonnealternance;

import java.util.List;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.config.LaBonneAlternanceProperties;
import com.cvanalyzer.backend.ingestion.IngestionSummary;
import com.cvanalyzer.backend.ingestion.JobPersistenceService;

@Service
public class LaBonneAlternanceIngestionService {
 private final LaBonneAlternanceClient client;private final LaBonneAlternanceJobMapper mapper;
 private final JobPersistenceService persistence;private final LaBonneAlternanceProperties properties;
 public LaBonneAlternanceIngestionService(LaBonneAlternanceClient client,
   LaBonneAlternanceJobMapper mapper,JobPersistenceService persistence,
   LaBonneAlternanceProperties properties){
  this.client=client;this.mapper=mapper;this.persistence=persistence;this.properties=properties;
 }
 public IngestionSummary ingest(){
  if(properties.requestBudget()<1)return new IngestionSummary(0,0,0,0,0);
  LaBonneAlternanceResponse response=client.search();
  List<LaBonneAlternanceResponse.JobOffer> jobs=response.jobs()==null?List.of():response.jobs();
  return persistence.persist("la_bonne_alternance",jobs,mapper::map);
 }
}
