package com.cvanalyzer.backend.ingestion.labonnealternance;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public record LaBonneAlternanceResponse(List<JobOffer> jobs,List<Object> recruiters,List<Warning> warnings){
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record JobOffer(Identifier identifier,Workplace workplace,Apply apply,Contract contract,
   Offer offer,@JsonProperty("is_delegated") Boolean delegated){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Identifier(@JsonProperty("partner_job_id") String partnerJobId,String id,
   @JsonProperty("partner_label") String partnerLabel){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Workplace(String name,String description,String website,String siret,Location location,
   String brand,@JsonProperty("legal_name") String legalName,String size,Domain domain){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Location(String address,GeoPoint geopoint){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record GeoPoint(List<Double> coordinates,String type){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Domain(Double idcc,String opco,Naf naf){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Naf(String code,String label){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Apply(String phone,String url,@JsonProperty("recipient_id") String recipientId){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Contract(String start,Integer duration,List<String> type,String remote,
   @JsonProperty("start_type") String startType,
   @JsonProperty("start_is_flexible") Boolean startIsFlexible){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Offer(String title,@JsonProperty("desired_skills") List<String> desiredSkills,
   @JsonProperty("to_be_acquired_skills") List<String> toBeAcquiredSkills,
   @JsonProperty("access_conditions") List<String> accessConditions,
   @JsonProperty("opening_count") Double openingCount,Publication publication,
   @JsonProperty("rome_codes") List<String> romeCodes,String description,
   @JsonProperty("target_diploma") TargetDiploma targetDiploma,String status,
   @JsonProperty("to_applicant_questions") List<String> toApplicantQuestions){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Publication(String creation,String expiration){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record TargetDiploma(String european,String label){}
 @JsonIgnoreProperties(ignoreUnknown=true)
 public record Warning(String message,String code){}
}
