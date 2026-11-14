package com.cvanalyzer.backend.dto;

import java.util.List;

public record CvParseResponse(String parsingStatus,String parserVersion,Profile profile,
 String professionalSummaryText,String skillsText,String experienceText,String projectsText,String educationText,
 String semanticProfileText,List<String> warnings){
 public record Profile(String targetRole,String professionalSummary,List<String> technicalSkills,
  Integer yearsOfExperience,List<Experience> experience,List<Project> projects,List<Education> education,
  List<String> languages){}
 public record Experience(String role,String organization,String period,String description){}
 public record Project(String name,String description,List<String> technologies){}
 public record Education(String qualification,String institution,String period,String description){}
}
