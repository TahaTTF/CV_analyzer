package com.cvanalyzer.backend.ingestion.labonnealternance;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.ingestion.ItJobClassifier;
import com.cvanalyzer.backend.model.JobDocument;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.model.JobStatus;

@Component
public class LaBonneAlternanceJobMapper {
 private final Clock clock;private final ItJobClassifier classifier;
 @Autowired
 public LaBonneAlternanceJobMapper(ItJobClassifier classifier){this(Clock.systemUTC(),classifier);}
 LaBonneAlternanceJobMapper(Clock clock,ItJobClassifier classifier){
  this.clock=clock;this.classifier=classifier;
 }
 public JobDocument map(LaBonneAlternanceResponse.JobOffer source){
  if(source==null)throw invalid("record is null");
  if(source.identifier()==null)throw invalid("identifier is missing");
  if(source.offer()==null)throw invalid("offer is missing");
  String partner=required(source.identifier().partnerLabel(),"identifier.partner_label");
  String partnerJobId=required(source.identifier().partnerJobId(),"identifier.partner_job_id");
  String externalId=partner+":"+partnerJobId;
  String title=required(source.offer().title(),"offer.title");
  String url=source.apply()==null?null:required(source.apply().url(),"apply.url");
  validateHttpUrl(url);
  String company=company(source.workplace());
  String address=source.workplace()==null||source.workplace().location()==null?null:
    blankToNull(source.workplace().location().address());
  List<String> romes=clean(source.offer().romeCodes());
  List<String> skills=clean(source.offer().desiredSkills());
  List<String> acquired=clean(source.offer().toBeAcquiredSkills());
  List<String> contractTypes=source.contract()==null?List.of():clean(source.contract().type());
  boolean remote=source.contract()!=null&&source.contract().remote()!=null
    &&!"onsite".equalsIgnoreCase(source.contract().remote())
    &&!"presentiel".equalsIgnoreCase(source.contract().remote());
  Instant now=clock.instant();
  Instant published=parseInstant(source.offer().publication()==null?null:
    source.offer().publication().creation());
  List<String> structured=concat(skills,contractTypes);
  var classification=classifier.classify(title,source.offer().description(),structured,romes);
  return new JobDocument(null,JobSource.LA_BONNE_ALTERNANCE,externalId,title,
    new JobDocument.Company(company),blankToNull(source.offer().description()),
    new JobDocument.Location(address,null,"FR",remote),
    new JobDocument.Requirements(skills,acquired),contractTypes,url,published,now,now,
    JobStatus.ACTIVE,new JobDocument.SourceMetadata(partner,partnerJobId,List.of(),List.of(),romes),
    classification);
 }
 private String company(LaBonneAlternanceResponse.Workplace w){
  if(w==null)return null;
  if(!blank(w.name()))return w.name().trim();
  if(!blank(w.brand()))return w.brand().trim();
  return blankToNull(w.legalName());
 }
 private List<String> concat(List<String> a,List<String> b){
  return java.util.stream.Stream.concat(a.stream(),b.stream()).distinct().toList();
 }
 private List<String> clean(List<String> values){return values==null?List.of():values.stream()
  .filter(Objects::nonNull).filter(v->!v.isBlank()).map(String::trim).distinct().toList();}
 private Instant parseInstant(String value){if(blank(value))return null;try{return Instant.parse(value);}
  catch(java.time.format.DateTimeParseException ex){try{return OffsetDateTime.parse(value).toInstant();}
  catch(java.time.format.DateTimeParseException ignored){throw invalid("offer.publication.creation is invalid");}}}
 private String required(String v,String f){if(blank(v))throw invalid("missing required field: "+f);return v.trim();}
 private String blankToNull(String v){return blank(v)?null:v.trim();}
 private boolean blank(String v){return v==null||v.isBlank();}
 private void validateHttpUrl(String value){try{URI u=URI.create(value);if(!u.isAbsolute()||
  (!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme())))
  throw invalid("url must use HTTP(S)");}catch(IllegalArgumentException ex){throw invalid("url is invalid");}}
 private ExternalJobValidationException invalid(String reason){
  return new ExternalJobValidationException("Invalid La bonne alternance job: "+reason);
 }
}
