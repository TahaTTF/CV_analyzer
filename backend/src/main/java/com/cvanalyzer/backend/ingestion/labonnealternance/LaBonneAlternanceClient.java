package com.cvanalyzer.backend.ingestion.labonnealternance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.cvanalyzer.backend.config.LaBonneAlternanceProperties;
import com.cvanalyzer.backend.exception.ExternalServiceException;
import com.cvanalyzer.backend.exception.ProviderConfigurationException;

@Component
public class LaBonneAlternanceClient {
 private final RestClient client;private final LaBonneAlternanceProperties properties;
 public LaBonneAlternanceClient(@Qualifier("laBonneAlternanceRestClient") RestClient client,
   LaBonneAlternanceProperties properties){this.client=client;this.properties=properties;}
 public LaBonneAlternanceResponse search(){
  if(properties.apiKey()==null||properties.apiKey().isBlank())
   throw new ProviderConfigurationException("La bonne alternance API key is not configured");
  try{
   LaBonneAlternanceResponse response=client.get().uri(builder->{
    var uri=builder.path("/job/v1/search");
    if(properties.romes()!=null&&!properties.romes().isBlank())
     uri.queryParam("romes",properties.romes());
    return uri.build();
   }).header(HttpHeaders.AUTHORIZATION,"Bearer "+properties.apiKey())
    .retrieve().body(LaBonneAlternanceResponse.class);
   if(response==null)throw new ExternalServiceException(
     "La bonne alternance returned an empty response",null);
   return response;
  }catch(RestClientException ex){
   throw new ExternalServiceException("La bonne alternance request failed",ex);
  }
 }
}
