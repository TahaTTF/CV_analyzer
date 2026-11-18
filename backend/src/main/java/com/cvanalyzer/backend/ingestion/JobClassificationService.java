package com.cvanalyzer.backend.ingestion;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.repository.JobRepository;

@Service
public class JobClassificationService {
 private final JobRepository repository;private final ItJobClassifier classifier;private final Clock clock;
 @Autowired
 public JobClassificationService(JobRepository repository,ItJobClassifier classifier){
  this(repository,classifier,Clock.systemUTC());
 }
 JobClassificationService(JobRepository repository,ItJobClassifier classifier,Clock clock){
  this.repository=repository;this.classifier=classifier;this.clock=clock;
 }
 public ClassificationSummary classifyUnclassified(){
  return classify(false);
 }
 public ClassificationSummary classifyOutdated(){
  return classify(true);
 }
 private ClassificationSummary classify(boolean includeOutdated){
  List<JobDocument> updates=new ArrayList<>();
  long examined=0,relevant=0,nonIt=0,ambiguous=0;
  for(JobDocument job:repository.findAll()){
   examined++;
   if(job.itClassification()!=null&&(!includeOutdated
     ||ItJobClassifier.VERSION.equals(job.itClassification().version())))continue;
   List<String> structured=new ArrayList<>();
   if(job.sourceMetadata()!=null){
    structured.addAll(job.sourceMetadata().categories());
    structured.addAll(job.sourceMetadata().tags());
   }
   if(job.requirements()!=null)structured.addAll(job.requirements().skills());
   List<String> romes=job.sourceMetadata()==null?List.of():job.sourceMetadata().romeCodes();
   JobDocument.ItClassification result=classifier.classify(job.title(),job.description(),structured,romes);
   if(Boolean.TRUE.equals(result.isItRelevant()))relevant++;
   else if(Boolean.FALSE.equals(result.isItRelevant()))nonIt++;
   else ambiguous++;
   updates.add(job.withClassification(result,clock.instant()));
  }
  if(!updates.isEmpty())repository.saveAll(updates);
  return new ClassificationSummary(examined,updates.size(),relevant,nonIt,ambiguous);
 }
}
