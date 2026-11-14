package com.cvanalyzer.backend.enrichment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobDataQualityAuditService {
 private static final Pattern HTML=Pattern.compile("(?is)<[a-z][^>]*>|&(?:amp|nbsp|quot|lt|gt|#\\d+|#x[0-9a-f]+);");
 private final JobRepository repository;
 public JobDataQualityAuditService(JobRepository repository){this.repository=repository;}
 public DataQualityAudit audit(){
  Map<JobSource,Counts> sourceCounts=new EnumMap<>(JobSource.class);
  Map<String,List<JobDocument>> identities=new LinkedHashMap<>();
  Map<String,List<JobDocument>> content=new LinkedHashMap<>();
  Map<String,List<JobDocument>> titles=new LinkedHashMap<>();
  long total=0;int pageNumber=0;Page<JobDocument> page;
  do{
   page=repository.findAll(PageRequest.of(pageNumber++,200,Sort.by("id")));
   for(JobDocument job:page){
    total++;sourceCounts.computeIfAbsent(job.source(),k->new Counts()).add(job);
    add(identities,key(job.source(),job.externalId()),job);
    add(content,key(job.source(),normalized(job.title()),company(job),normalized(job.description()),job.url()),job);
    add(titles,key(job.source(),normalized(job.title())),job);
   }
  }while(page.hasNext());
  List<DataQualityAudit.SourceQuality> bySource=sourceCounts.entrySet().stream()
   .map(e->e.getValue().result(e.getKey())).toList();
  List<DataQualityAudit.DuplicateExample> examples=new ArrayList<>();
  long same=countGroups(identities,"SAME_SOURCE_EXTERNAL_ID",examples);
  long exact=countDifferentIds(content,"IDENTICAL_CONTENT_DIFFERENT_ID",examples);
  long repeated=countDifferentIds(titles,"REPEATED_TITLE",examples);
  return new DataQualityAudit(total,bySource,new DataQualityAudit.DuplicateSummary(
   same,exact,repeated,List.copyOf(examples)));
 }
 private long countGroups(Map<String,List<JobDocument>> groups,String kind,
   List<DataQualityAudit.DuplicateExample> examples){
  long count=0;
  for(List<JobDocument> jobs:groups.values())if(jobs.size()>1){count++;example(kind,jobs,examples);}
  return count;
 }
 private long countDifferentIds(Map<String,List<JobDocument>> groups,String kind,
   List<DataQualityAudit.DuplicateExample> examples){
  long count=0;
  for(List<JobDocument> jobs:groups.values())if(jobs.stream().map(JobDocument::externalId).distinct().count()>1){
   count++;example(kind,jobs,examples);
  }
  return count;
 }
 private void example(String kind,List<JobDocument> jobs,List<DataQualityAudit.DuplicateExample> examples){
  if(examples.size()>=20)return;
  JobDocument first=jobs.getFirst();
  examples.add(new DataQualityAudit.DuplicateExample(kind,first.source(),first.title(),
   jobs.stream().map(JobDocument::externalId).distinct().toList()));
 }
 private static void add(Map<String,List<JobDocument>> map,String key,JobDocument job){
  map.computeIfAbsent(key,k->new ArrayList<>()).add(job);
 }
 private static String key(Object...values){return java.util.Arrays.stream(values)
  .map(String::valueOf).collect(java.util.stream.Collectors.joining("\u001f"));}
 private static String company(JobDocument job){return job.company()==null?null:job.company().name();}
 private static String normalized(String value){if(value==null)return "";return Normalizer.normalize(value,
  Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
  .replaceAll("\\s+"," ").strip();}

 private static final class Counts{
  long total,it,nonIt,ambiguous,unclassified,missingDescriptions,htmlDescriptions,
   emptySkills,missingCity,missingCountry,unknownWorkMode;
  void add(JobDocument job){
   total++;
   if(job.itClassification()==null)unclassified++;
   else if(Boolean.TRUE.equals(job.itClassification().isItRelevant()))it++;
   else if(Boolean.FALSE.equals(job.itClassification().isItRelevant()))nonIt++;
   else ambiguous++;
   if(job.description()==null||job.description().isBlank())missingDescriptions++;
   else if(HTML.matcher(job.description()).find())htmlDescriptions++;
   if(job.enrichment()==null||job.enrichment().requiredSkills().isEmpty())emptySkills++;
   JobDocument.NormalizedLocation normalized=job.enrichment()==null?null:job.enrichment().normalizedLocation();
   String city=normalized==null?(job.location()==null?null:job.location().city()):normalized.city();
   String country=normalized==null?(job.location()==null?null:job.location().countryCode()):normalized.countryCode();
   if(city==null||city.isBlank())missingCity++;
   if(country==null||country.isBlank())missingCountry++;
   if(normalized==null||normalized.workMode()==null||normalized.workMode()==JobDocument.WorkMode.UNKNOWN)
    unknownWorkMode++;
  }
  DataQualityAudit.SourceQuality result(JobSource source){return new DataQualityAudit.SourceQuality(
   source,total,it,nonIt,ambiguous,unclassified,missingDescriptions,htmlDescriptions,emptySkills,
   missingCity,missingCountry,unknownWorkMode);}
 }
}
