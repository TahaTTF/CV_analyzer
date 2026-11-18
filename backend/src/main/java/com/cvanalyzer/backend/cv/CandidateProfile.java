package com.cvanalyzer.backend.cv;

import java.util.List;

public record CandidateProfile(String targetRole,String professionalSummary,
 List<String> technicalSkills,Integer yearsOfExperience,List<ExperienceEntry> experience,
 List<ProjectEntry> projects,List<EducationEntry> education,List<String> languages,
 List<AdditionalSection> additionalSections){
 public CandidateProfile{
  technicalSkills=technicalSkills==null?List.of():List.copyOf(technicalSkills);
  experience=experience==null?List.of():List.copyOf(experience);
  projects=projects==null?List.of():List.copyOf(projects);
  education=education==null?List.of():List.copyOf(education);
  languages=languages==null?List.of():List.copyOf(languages);
  additionalSections=additionalSections==null?List.of():List.copyOf(additionalSections);
 }
 public record ExperienceEntry(String role,String organization,String period,String description){}
 public record ProjectEntry(String name,String description,List<String> technologies){
  public ProjectEntry{technologies=technologies==null?List.of():List.copyOf(technologies);}
 }
 public record EducationEntry(String qualification,String institution,String period,String description){}
 public record AdditionalSection(String heading,String text){}
}
