package com.cvanalyzer.backend.ingestion.adzuna;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.cvanalyzer.backend.config.AdzunaProperties;
import com.cvanalyzer.backend.exception.ExternalServiceException;
import com.cvanalyzer.backend.exception.ProviderConfigurationException;

@Component
public class AdzunaClient {
 private final RestClient client;
 private final AdzunaProperties properties;
 public AdzunaClient(@Qualifier("adzunaRestClient") RestClient client,AdzunaProperties properties){
  this.client=client;this.properties=properties;
 }
 public AdzunaResponse fetchPage(int page){
  requireCredentials();
  try{
   AdzunaResponse response=client.get().uri(builder->{
    var uri=builder.path("/jobs/{country}/search/{page}")
      .queryParam("app_id",properties.appId()).queryParam("app_key",properties.appKey())
      .queryParam("results_per_page",properties.resultsPerPage())
      .queryParam("category","it-jobs").queryParam("content-type","application/json");
    if(properties.query()!=null&&!properties.query().isBlank())uri.queryParam("what",properties.query());
    return uri.build(properties.country(),page);
   }).retrieve().body(AdzunaResponse.class);
   if(response==null)throw new ExternalServiceException("Adzuna returned an empty response",null);
   return response;
  }catch(RestClientException ex){throw new ExternalServiceException("Adzuna request failed",ex);}
 }
 private void requireCredentials(){
  if(blank(properties.appId())||blank(properties.appKey()))
   throw new ProviderConfigurationException("Adzuna credentials are not configured");
 }
 private boolean blank(String value){return value==null||value.isBlank();}
}
