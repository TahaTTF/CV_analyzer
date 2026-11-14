package com.cvanalyzer.backend.enrichment;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.cvanalyzer.backend.model.JobDocument;

@Component
public class TechnicalSkillExtractor {
 private static final List<SkillDefinition> DICTIONARY=List.of(
  skill("Python","python"),sensitive("Java","Java"),skill("JavaScript","javascript","ecmascript"),
  skill("TypeScript","typescript"),sensitive("C","C"),skill("C++","c++"),skill("C#","c#","c sharp"),
  sensitive("Go","Go","Golang"),skill("Rust","rustlang","rust language"),skill("PHP","php"),
  skill("Kotlin","kotlin"),sensitive("Swift","Swift"),skill("Angular","angular","angularjs"),
  sensitive("React","React","React.js","ReactJS"),skill("Vue.js","vue.js","vuejs"),
  skill("HTML","html","html5"),skill("CSS","css","css3"),skill("Next.js","next.js","nextjs"),
  skill("Spring Boot","spring boot"),skill("Spring Framework","spring framework"),
  skill("Node.js","node.js","nodejs"),skill("Express.js","express.js","expressjs"),
  skill("Django","django"),skill("Flask","flask"),skill("FastAPI","fastapi"),
  skill(".NET",".net","dotnet"),skill("ASP.NET","asp.net","aspnet"),skill("SQL","sql"),
  skill("PostgreSQL","postgresql","postgres"),
  skill("MySQL","mysql"),skill("MongoDB","mongodb"),skill("Redis","redis"),
  skill("Oracle Database","oracle database","oracle db"),skill("SQLite","sqlite"),
  skill("Docker","docker"),skill("Kubernetes","kubernetes","k8s"),skill("Git","git"),skill("GitHub","github"),
  skill("GitHub Actions","github actions"),skill("Jenkins","jenkins"),
  skill("CI/CD","ci/cd","ci cd","continuous integration","continuous delivery"),
  skill("AWS","aws","amazon web services"),skill("Azure","azure","microsoft azure"),
  skill("Google Cloud","google cloud","gcp"),skill("Terraform","terraform"),skill("Linux","linux"),
  skill("Maven","maven"),skill("Postman","postman"),skill("Android","android"),
  skill("REST APIs","rest api","rest apis","restful api","restful apis"),
  skill("Pandas","pandas"),skill("NumPy","numpy"),skill("PyTorch","pytorch"),
  skill("TensorFlow","tensorflow"),skill("Scikit-learn","scikit-learn","scikit learn","sklearn"),
  skill("YOLOv8","yolov8","yolo v8"),skill("OpenCV","opencv","open cv"),
  skill("Machine Learning","machine learning","apprentissage automatique","maschinelles lernen"),
  skill("Deep Learning","deep learning","apprentissage profond","tiefes lernen"),
  skill("NLP","nlp","natural language processing","traitement automatique du langage"),
  skill("Computer Vision","computer vision","vision par ordinateur","maschinelles sehen"),
  skill("Power BI","power bi","powerbi"),skill("SSIS","ssis","sql server integration services"),
  skill("SSAS","ssas","sql server analysis services"),skill("Spark","apache spark"),skill("Kafka","apache kafka"),
  skill("Cybersecurity","cybersecurity","cyber security","cybersecurite","cybersicherheit"),
  skill("SIEM","siem"),skill("SOC","security operations center","centre operationnel de securite"),
  skill("Penetration Testing","penetration testing","pentest","test d intrusion"),
  skill("TCP/IP","tcp/ip","tcp ip"),skill("DNS","dns"),skill("Firewalls","firewall","firewalls",
   "pare feu"),skill("Network Administration","network administration","administration reseau",
   "netzwerkadministration"),skill("JUnit","junit"),skill("Selenium","selenium"),
  skill("Cypress","cypress"),skill("Playwright","playwright"),
  skill("QA Automation","qa automation","test automation","automatisation des tests","testautomatisierung"),
  skill("SAP","sap"),skill("Salesforce","salesforce"),skill("ERP","erp"),
  skill("Microsoft Dynamics","microsoft dynamics","dynamics 365"));

 public Extraction extract(JobDocument job,String descriptionText){
  Map<String,Set<String>> required=new LinkedHashMap<>();
  Map<String,Set<String>> training=new LinkedHashMap<>();
  addMatches(required,job.title(),"title");
  addMatches(required,descriptionText,"description");
  if(job.requirements()!=null){
   for(String value:job.requirements().skills())addMatches(required,value,"provider-field");
   for(String value:job.requirements().toBeAcquiredSkills())addMatches(training,value,"provider-training");
  }
  if(job.sourceMetadata()!=null)
   for(String tag:job.sourceMetadata().tags())addMatches(required,tag,"provider-tag");
  return new Extraction(toSkills(required),toSkills(training));
 }

 public List<JobDocument.ExtractedSkill> extractFromText(String title,String text){
  Map<String,Set<String>> values=new LinkedHashMap<>();
  addMatches(values,title,"title");
  addMatches(values,text,"cv-text");
  return toSkills(values);
 }

 private void addMatches(Map<String,Set<String>> result,String text,String evidence){
  if(text==null||text.isBlank())return;
  String normalized=normalize(text);
  for(SkillDefinition definition:DICTIONARY){
   String candidate=definition.caseSensitive?text:normalized;
   if(definition.pattern.matcher(candidate).find())
    result.computeIfAbsent(definition.canonical,k->new LinkedHashSet<>()).add(evidence);
  }
 }
 private List<JobDocument.ExtractedSkill> toSkills(Map<String,Set<String>> values){
  return values.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
   .map(e->new JobDocument.ExtractedSkill(e.getKey(),List.copyOf(e.getValue()))).toList();
 }
 private static SkillDefinition skill(String canonical,String...aliases){return definition(canonical,false,aliases);}
 private static SkillDefinition sensitive(String canonical,String...aliases){return definition(canonical,true,aliases);}
 private static SkillDefinition definition(String canonical,boolean caseSensitive,String...aliases){
  List<String> patterns=new ArrayList<>();
  for(String alias:aliases){
   String value=caseSensitive?alias:normalize(alias);
   patterns.add("(?<![\\p{L}\\p{N}_])"+Pattern.quote(value)+"(?![\\p{L}\\p{N}_+#])");
  }
  int flags=caseSensitive?Pattern.UNICODE_CASE:Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE;
  return new SkillDefinition(canonical,Pattern.compile(String.join("|",patterns),flags),caseSensitive);
 }
 private static String normalize(String value){
  return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}+","")
   .toLowerCase(Locale.ROOT);
 }
 public record Extraction(List<JobDocument.ExtractedSkill> required,
  List<JobDocument.ExtractedSkill> toBeAcquired){}
 private record SkillDefinition(String canonical,Pattern pattern,boolean caseSensitive){}
}
