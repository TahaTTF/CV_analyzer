package com.cvanalyzer.backend.recommendation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cvanalyzer.backend.cv.CvParsingService;
import com.cvanalyzer.backend.dto.CvParseResponse;
import com.cvanalyzer.backend.dto.RecommendationDtos;
import com.cvanalyzer.backend.exception.RecommendationException;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobStatus;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class RecommendationService {
 private static final Logger log=LoggerFactory.getLogger(RecommendationService.class);
 private final CvParsingService parser;private final JobRepository jobs;private final AiMatchingClient ai;
 public RecommendationService(CvParsingService parser,JobRepository jobs,AiMatchingClient ai){
  this.parser=parser;this.jobs=jobs;this.ai=ai;}
 public RecommendationDtos.Response recommend(MultipartFile file,int topK){
  long totalStarted=System.nanoTime();long stageStarted=totalStarted;
  CvParseResponse parsed=parser.parse(file);long parseNanos=System.nanoTime()-stageStarted;
  stageStarted=System.nanoTime();List<JobDocument> storedJobs=jobs.findAll();
  long queryNanos=System.nanoTime()-stageStarted;stageStarted=System.nanoTime();
  List<JobDocument> candidates=storedJobs.stream()
   .filter(job->job.status()==JobStatus.ACTIVE)
   .filter(job->job.itClassification()==null||!Boolean.FALSE.equals(job.itClassification().isItRelevant()))
   .toList();
  if(candidates.isEmpty())throw new RecommendationException(HttpStatus.NOT_FOUND,"NO_JOBS_AVAILABLE",
   "No active IT job candidates are available");
  List<RecommendationDtos.AiJobCandidate> aiJobs=candidates.stream().map(this::candidate).toList();
  var request=new RecommendationDtos.AiMatchRequest(parsed.semanticProfileText(),
   parsed.profile().technicalSkills(),parsed.profile().targetRole(),aiJobs,topK);
  long prepareNanos=System.nanoTime()-stageStarted;stageStarted=System.nanoTime();
  var matched=ai.match(request);long aiNanos=System.nanoTime()-stageStarted;
  stageStarted=System.nanoTime();Map<String,JobDocument> byId=new LinkedHashMap<>();
  candidates.forEach(job->byId.put(job.id(),job));List<RecommendationDtos.Recommendation> result=new ArrayList<>();
  for(var match:matched.recommendations()){
   JobDocument job=byId.get(match.jobId());if(job==null)throw new RecommendationException(HttpStatus.BAD_GATEWAY,
    "AI_RESPONSE_INVALID","AI matching service returned an unknown job identifier");
   result.add(new RecommendationDtos.Recommendation(job.id(),job.title(),
    job.company()==null?null:job.company().name(),job.location()==null?null:job.location().displayName(),
    job.source()==null?null:job.source().name(),job.url(),match.matchScore(),match.semanticScore(),
    match.skillScore(),match.roleScore(),match.matchedSkills(),match.missingSkills(),match.explanation()));
  }
  var response=new RecommendationDtos.Response(parsed.parserVersion(),matched.modelVersion(),parsed.warnings(),result);
  long responseNanos=System.nanoTime()-stageStarted;long totalNanos=System.nanoTime()-totalStarted;
  log.info("recommendation_complete candidates={} topK={} parseMs={} mongoMs={} prepareMs={} aiMs={} responseMs={} totalMs={}",
   candidates.size(),topK,millis(parseNanos),millis(queryNanos),millis(prepareNanos),millis(aiNanos),
   millis(responseNanos),millis(totalNanos));
  return response;
 }
 private RecommendationDtos.AiJobCandidate candidate(JobDocument job){
  String description=job.enrichment()!=null&&job.enrichment().descriptionText()!=null
   ?job.enrichment().descriptionText():job.description();
  List<String> skills=job.enrichment()!=null&&!job.enrichment().requiredSkills().isEmpty()
   ?job.enrichment().requiredSkills().stream().map(JobDocument.ExtractedSkill::name).toList()
   :job.requirements().skills();List<String> categories=new ArrayList<>();
  if(job.sourceMetadata()!=null){categories.addAll(job.sourceMetadata().categories());categories.addAll(job.sourceMetadata().tags());}
  return new RecommendationDtos.AiJobCandidate(job.id(),job.title(),description==null?"":description,skills,categories);
 }
 private long millis(long nanos){return TimeUnit.NANOSECONDS.toMillis(nanos);}
}
