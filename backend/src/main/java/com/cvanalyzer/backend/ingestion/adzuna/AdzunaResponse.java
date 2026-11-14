package com.cvanalyzer.backend.ingestion.adzuna;

import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public record AdzunaResponse(Long count,List<AdzunaJob> results){
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record AdzunaJob(String id,String title,String description,Instant created,
  @JsonProperty("redirect_url") String redirectUrl,Company company,Location location,
  Category category,@JsonProperty("contract_type") String contractType,
  @JsonProperty("contract_time") String contractTime){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Company(@JsonProperty("display_name") String displayName){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Location(@JsonProperty("display_name") String displayName,List<String> area){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Category(String label,String tag){}
}
