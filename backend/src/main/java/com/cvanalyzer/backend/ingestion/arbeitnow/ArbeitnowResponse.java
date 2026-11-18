package com.cvanalyzer.backend.ingestion.arbeitnow;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
@JsonIgnoreProperties(ignoreUnknown=true)
public record ArbeitnowResponse(List<ArbeitnowJob> data,Links links,Meta meta) {
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record ArbeitnowJob(String slug,@JsonProperty("company_name") String companyName,
   String title,String description,Boolean remote,String url,List<String> tags,
   @JsonProperty("job_types") List<String> jobTypes,String location,
   @JsonProperty("created_at") Long createdAt){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Links(String first,String last,String prev,String next){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Meta(@JsonProperty("current_page") Integer currentPage,
   @JsonProperty("per_page") Integer perPage,String terms,String info){}
}
