package com.cvanalyzer.backend.enrichment;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobEnrichmentBatchService {
 private final JobRepository repository;
 private final LocalJobEnrichmentPort enrichment;
 public JobEnrichmentBatchService(JobRepository repository,LocalJobEnrichmentPort enrichment){
  this.repository=repository;this.enrichment=enrichment;
 }
 public EnrichmentSummary run(boolean dryRun,int batchSize){
  if(batchSize<1||batchSize>500)throw new IllegalArgumentException("batchSize must be between 1 and 500");
  MutableStats stats=new MutableStats(dryRun);
  int pageNumber=0;
  Page<JobDocument> page;
  do{
   page=repository.findAll(PageRequest.of(pageNumber,batchSize,Sort.by("id")));
   List<JobDocument> writes=new ArrayList<>();
   for(JobDocument job:page.getContent()){
    stats.examined++;
    try{
     JobDocument result=enrichment.enrich(job);
     if(result==job||result.equals(job)){stats.unchanged++;continue;}
     stats.wouldUpdate++;
     countQuality(result,stats);
     if(!dryRun)writes.add(result);
    }catch(RuntimeException ex){stats.failed++;}
   }
   if(!dryRun&&!writes.isEmpty()){
    repository.saveAll(writes);stats.updated+=writes.size();
   }
   pageNumber++;
  }while(page.hasNext());
  return stats.result();
 }
 private void countQuality(JobDocument job,MutableStats stats){
  var value=job.enrichment();
  if(value==null)return;
  if(value.descriptionText()!=null&&!value.descriptionText().isBlank())stats.cleaned++;
  if(!value.requiredSkills().isEmpty()||!value.toBeAcquiredSkills().isEmpty())stats.skills++;
  var location=value.normalizedLocation();
  if(location!=null&&(location.city()!=null||location.countryCode()!=null
    ||location.workMode()!=JobDocument.WorkMode.UNKNOWN))stats.locations++;
 }
 private static final class MutableStats{
  final boolean dryRun;long examined,wouldUpdate,updated,unchanged,failed,cleaned,skills,locations;
  MutableStats(boolean dryRun){this.dryRun=dryRun;}
  EnrichmentSummary result(){return new EnrichmentSummary(dryRun,examined,wouldUpdate,updated,
   unchanged,failed,cleaned,skills,locations,LocalJobEnrichmentService.VERSION);}
 }
}
