package com.cvanalyzer.backend.cv;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SemanticProfileComposer {
 private final PersonalDataRedactor redactor;
 public SemanticProfileComposer(PersonalDataRedactor redactor){this.redactor=redactor;}
 public NlpTextRepresentation compose(CandidateProfile profile){
  String summary=sentence(redactor.redact(profile.professionalSummary()));
  String skills=profile.technicalSkills().isEmpty()?null:
   "Technical skills: "+String.join(", ",profile.technicalSkills())+".";
  String experience=profile.experience().isEmpty()?null:profile.experience().stream()
   .map(this::experience).filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" | "));
  if(experience!=null)experience="Professional experience: "+experience;
  String projects=profile.projects().isEmpty()?null:profile.projects().stream()
   .map(this::project).filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" | "));
  if(projects!=null)projects="Projects: "+projects;
  String education=profile.education().isEmpty()?null:profile.education().stream()
   .map(this::education).filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" | "));
  if(education!=null)education="Education: "+education;
  String additional=profile.additionalSections().isEmpty()?null:profile.additionalSections().stream()
   .map(this::additional).filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" "));
  LinkedHashSet<String> semantic=new LinkedHashSet<>();
  add(semantic,sentence(redactor.redact(profile.targetRole())));add(semantic,summary);add(semantic,skills);
  add(semantic,experience);add(semantic,projects);add(semantic,education);add(semantic,additional);
  return new NlpTextRepresentation(summary,skills,experience,projects,education,
   semantic.isEmpty()?null:deduplicateSentences(String.join(" ",semantic)));
 }
 private String experience(CandidateProfile.ExperienceEntry value){LinkedHashSet<String> parts=new LinkedHashSet<>();
  add(parts,value.role());add(parts,value.organization());add(parts,value.period());add(parts,limited(value.description()));
  return sentence(redactor.redact(String.join(". ",parts)));}
 private String project(CandidateProfile.ProjectEntry value){LinkedHashSet<String> parts=new LinkedHashSet<>();
  add(parts,value.name());add(parts,limited(value.description()));
  if(!value.technologies().isEmpty())add(parts,"Technologies: "+String.join(", ",value.technologies()));
  return sentence(redactor.redact(String.join(". ",parts)));}
 private String education(CandidateProfile.EducationEntry value){LinkedHashSet<String> parts=new LinkedHashSet<>();
  add(parts,value.qualification());add(parts,value.institution());add(parts,value.period());
  add(parts,limited(value.description()));
  return sentence(redactor.redact(String.join(". ",parts)));}
 private String additional(CandidateProfile.AdditionalSection value){return sentence(redactor.redact(
  value.heading()+": "+limited(value.text())));}
 private String limited(String value){if(value==null)return null;String clean=value.replaceAll("\\s+"," ").strip();
  return clean.length()<=700?clean:clean.substring(0,700).strip();}
 private String sentence(String value){if(value==null||value.isBlank())return null;String clean=value.replace('\n',' ')
  .replaceAll("\\s+"," ").strip();return clean.matches(".*[.!?]$")?clean:clean+".";}
 private String deduplicateSentences(String value){LinkedHashSet<String> seen=new LinkedHashSet<>();List<String> kept=new ArrayList<>();
  for(String sentence:value.replaceAll("\\s+"," ").strip().split("(?<=[.!?])\\s+")){
   String key=sentence.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").strip();
   if(!key.isBlank()&&seen.add(key))kept.add(sentence);}
  return String.join(" ",kept);}
 private void add(java.util.Collection<String> values,String value){if(value!=null&&!value.isBlank())values.add(value);}
}
