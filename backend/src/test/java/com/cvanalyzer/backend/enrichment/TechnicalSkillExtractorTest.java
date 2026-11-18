package com.cvanalyzer.backend.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.model.*;

class TechnicalSkillExtractorTest {
 private final TechnicalSkillExtractor extractor=new TechnicalSkillExtractor();

 @Test void distinguishesJavaScriptJavaCAndSqlFamiliesAndPunctuation(){
  var result=extractor.extract(job("JavaScript Developer",
   "C++ C# .NET Node.js PostgreSQL AWS Azure"),"C++ C# .NET Node.js PostgreSQL AWS Azure");
  assertThat(names(result.required())).contains("JavaScript","C++","C#",".NET","Node.js",
   "PostgreSQL","AWS","Azure").doesNotContain("Java","C","SQL");
 }
 @Test void extractsFrenchAndGermanAliasesAndEvidence(){
  String text="Apprentissage automatique, cybersecurite et administration reseau. "
   +"Maschinelles Lernen und Cybersicherheit.";
  var result=extractor.extract(job("Ingenieur",text),text);
  assertThat(names(result.required())).contains("Machine Learning","Cybersecurity","Network Administration");
  assertThat(result.required()).allSatisfy(skill->assertThat(skill.evidenceSources()).contains("description"));
 }
 @Test void avoidsOrdinaryWordsAndSubstringFalsePositives(){
  String text="We go fast, react to change, drink java coffee and discuss rust on metal.";
  var result=extractor.extract(job("JavaScript Developer",text),text);
  assertThat(names(result.required())).contains("JavaScript")
   .doesNotContain("Go","React","Java","Rust","C");
 }
 @Test void separatesProviderTrainingSkillsFromRequiredSkills(){
  JobDocument base=job("Backend Developer","Uses Java");
  base=new JobDocument(base.id(),base.source(),base.externalId(),base.title(),base.company(),
   base.description(),base.location(),new JobDocument.Requirements(List.of("Java"),List.of("Docker")),
   base.employmentTypes(),base.url(),base.publishedAt(),base.fetchedAt(),base.updatedAt(),base.status(),
   base.sourceMetadata(),base.itClassification());
  var result=extractor.extract(base,base.description());
  assertThat(names(result.required())).contains("Java");
  assertThat(names(result.toBeAcquired())).containsExactly("Docker");
  assertThat(result.toBeAcquired().getFirst().evidenceSources()).containsExactly("provider-training");
 }
 @Test void extractsCvToolingComputerVisionAndDataPlatformSkillsExactly(){
  String text="YOLOv8 and OpenCV; SSIS and SSAS; Maven, Postman, Android, GitHub and REST APIs.";
  assertThat(names(extractor.extractFromText(null,text))).contains("YOLOv8","OpenCV","SSIS","SSAS",
   "Maven","Postman","Android","GitHub","REST APIs").doesNotContain("Git","GitHub Actions");
 }
 private List<String> names(List<JobDocument.ExtractedSkill> values){return values.stream()
  .map(JobDocument.ExtractedSkill::name).toList();}
 private JobDocument job(String title,String description){
  Instant now=Instant.parse("2026-10-21T10:00:00Z");
  return new JobDocument("id",JobSource.ARBEITNOW,"ext",title,new JobDocument.Company("Acme"),
   description,new JobDocument.Location("Berlin",null,"DE",false),new JobDocument.Requirements(List.of()),
   List.of(),"https://example.test/job",now,now,now,JobStatus.ACTIVE,null,null);
 }
}
