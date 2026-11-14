package com.cvanalyzer.backend.model;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("jobs")
@CompoundIndexes({
 @CompoundIndex(name="source_external_id_unique",def="{'source':1,'externalId':1}",unique=true),
 @CompoundIndex(name="status_fetched_at",def="{'status':1,'fetchedAt':-1}"),
 @CompoundIndex(name="status_it_published_at",def="{'status':1,'itClassification.isItRelevant':1,'publishedAt':-1}")
})
public record JobDocument(@Id String id, JobSource source, String externalId, String title,
 Company company, String description, Location location, Requirements requirements,
 List<String> employmentTypes, String url, @Indexed Instant publishedAt, Instant fetchedAt,
 Instant updatedAt, JobStatus status, SourceMetadata sourceMetadata,
 ItClassification itClassification, JobEnrichment enrichment) {
 public JobDocument {
  requirements=requirements==null?new Requirements(List.of(),List.of()):requirements;
  employmentTypes=employmentTypes==null?List.of():List.copyOf(employmentTypes);
 }
 public JobDocument(String id, JobSource source, String externalId, String title,
  Company company, String description, Location location, Requirements requirements,
  List<String> employmentTypes, String url, Instant publishedAt, Instant fetchedAt,
  Instant updatedAt, JobStatus status) {
  this(id,source,externalId,title,company,description,location,requirements,employmentTypes,
   url,publishedAt,fetchedAt,updatedAt,status,null,null,null);
 }
 public JobDocument(String id, JobSource source, String externalId, String title,
  Company company, String description, Location location, Requirements requirements,
  List<String> employmentTypes, String url, Instant publishedAt, Instant fetchedAt,
  Instant updatedAt, JobStatus status, SourceMetadata sourceMetadata,
  ItClassification itClassification) {
  this(id,source,externalId,title,company,description,location,requirements,employmentTypes,
   url,publishedAt,fetchedAt,updatedAt,status,sourceMetadata,itClassification,null);
 }
 public boolean hasSameSourceContent(JobDocument other) {
  return other!=null&&Objects.equals(title,other.title)&&Objects.equals(company,other.company)
   &&Objects.equals(description,other.description)&&Objects.equals(location,other.location)
   &&Objects.equals(requirements,other.requirements)&&Objects.equals(employmentTypes,other.employmentTypes)
   &&Objects.equals(url,other.url)&&Objects.equals(publishedAt,other.publishedAt)
   &&Objects.equals(sourceMetadata,other.sourceMetadata)&&status==other.status;
 }
 public JobDocument asUpdateOf(JobDocument old,Instant now) {
  return new JobDocument(old.id,source,externalId,title,company,description,location,requirements,
   employmentTypes,url,publishedAt,now,now,status,sourceMetadata,itClassification,enrichment);
 }
 public JobDocument withClassification(ItClassification classification,Instant now){
  return new JobDocument(id,source,externalId,title,company,description,location,requirements,
   employmentTypes,url,publishedAt,fetchedAt,now,status,sourceMetadata,classification,enrichment);
 }
 public JobDocument withEnrichment(JobEnrichment value,Instant now){
  return new JobDocument(id,source,externalId,title,company,description,location,requirements,
   employmentTypes,url,publishedAt,fetchedAt,now,status,sourceMetadata,itClassification,value);
 }
 public record Company(String name){}
 public record Location(String displayName,String city,String countryCode,boolean remote){}
 public record Requirements(List<String> skills,List<String> toBeAcquiredSkills){
  public Requirements(List<String> skills){this(skills,List.of());}
  public Requirements{
   skills=skills==null?List.of():List.copyOf(skills);
   toBeAcquiredSkills=toBeAcquiredSkills==null?List.of():List.copyOf(toBeAcquiredSkills);
  }
 }
 public record SourceMetadata(String partnerLabel,String partnerJobId,List<String> categories,
  List<String> tags,List<String> romeCodes){
  public SourceMetadata{
   categories=categories==null?List.of():List.copyOf(categories);
   tags=tags==null?List.of():List.copyOf(tags);
   romeCodes=romeCodes==null?List.of():List.copyOf(romeCodes);
  }
 }
 public record ItClassification(Boolean isItRelevant,List<String> matchedCategories,
  String relevanceReason,String version,Instant classifiedAt){
  public ItClassification{
   matchedCategories=matchedCategories==null?List.of():List.copyOf(matchedCategories);
  }
 }
 public record JobEnrichment(String descriptionText,List<ExtractedSkill> requiredSkills,
  List<ExtractedSkill> toBeAcquiredSkills,NormalizedLocation normalizedLocation,
  String version,Instant enrichedAt){
  public JobEnrichment{
   requiredSkills=requiredSkills==null?List.of():List.copyOf(requiredSkills);
   toBeAcquiredSkills=toBeAcquiredSkills==null?List.of():List.copyOf(toBeAcquiredSkills);
  }
 }
 public record ExtractedSkill(String name,List<String> evidenceSources){
  public ExtractedSkill{
   evidenceSources=evidenceSources==null?List.of():List.copyOf(evidenceSources);
  }
 }
 public record NormalizedLocation(String original,String city,String region,String countryCode,
  WorkMode workMode,Double confidence){}
 public enum WorkMode{REMOTE,HYBRID,ON_SITE,UNKNOWN}
}
