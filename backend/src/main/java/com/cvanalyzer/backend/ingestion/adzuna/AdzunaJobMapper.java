package com.cvanalyzer.backend.ingestion.adzuna;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Arrays;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.ingestion.ItJobClassifier;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.model.JobStatus;

@Component
public class AdzunaJobMapper {
 private final Clock clock; private final ItJobClassifier classifier;
 @Autowired
 public AdzunaJobMapper(ItJobClassifier classifier){this(Clock.systemUTC(),classifier);}
 AdzunaJobMapper(Clock clock,ItJobClassifier classifier){this.clock=clock;this.classifier=classifier;}
 public JobDocument map(AdzunaResponse.AdzunaJob source){
  if(source==null)throw invalid("record is null");
  String id=required(source.id(),"id"),title=required(source.title(),"title");
  String company=source.company()==null?null:blankToNull(source.company().displayName());
  String url=required(source.redirectUrl(),"redirect_url");validateHttpUrl(url);
  String location=source.location()==null?null:blankToNull(source.location().displayName());
  String country=first(source.location()==null?null:source.location().area());
  List<String> categories=source.category()==null?List.of():clean(
    Arrays.asList(source.category().label(),source.category().tag()));
  List<String> employment=clean(Arrays.asList(source.contractType(),source.contractTime()));
  Instant now=clock.instant();
  var classification=classifier.classify(title,source.description(),categories,List.of());
  return new JobDocument(null,JobSource.ADZUNA,id,title,new JobDocument.Company(company),
    blankToNull(source.description()),new JobDocument.Location(location,null,country,false),
    new JobDocument.Requirements(List.of()),employment,url,source.created(),now,now,JobStatus.ACTIVE,
    new JobDocument.SourceMetadata("Adzuna",id,categories,List.of(),List.of()),classification);
 }
 private List<String> clean(List<String> values){return values.stream().filter(Objects::nonNull)
  .filter(v->!v.isBlank()).map(String::trim).distinct().toList();}
 private String first(List<String> values){return values==null||values.isEmpty()?null:values.getFirst();}
 private String required(String v,String f){if(v==null||v.isBlank())throw invalid("missing required field: "+f);return v.trim();}
 private String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
 private void validateHttpUrl(String value){try{URI u=URI.create(value);if(!u.isAbsolute()||
  (!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme())))
  throw invalid("url must use HTTP(S)");}catch(IllegalArgumentException ex){throw invalid("url is invalid");}}
 private ExternalJobValidationException invalid(String reason){
  return new ExternalJobValidationException("Invalid Adzuna job: "+reason);
 }
}
