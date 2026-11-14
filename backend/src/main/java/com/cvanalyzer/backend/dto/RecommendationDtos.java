package com.cvanalyzer.backend.dto;

import java.util.List;

public final class RecommendationDtos {
 private RecommendationDtos(){}
 public record AiMatchRequest(String semanticProfile,List<String> skills,String targetRole,
  List<AiJobCandidate> jobs,int topK){}
 public record AiJobCandidate(String jobId,String title,String description,List<String> skills,
  List<String> categories){}
 public record AiMatchResponse(String modelVersion,List<AiMatchResult> recommendations){}
 public record AiMatchResult(String jobId,double matchScore,double semanticScore,double skillScore,
  double roleScore,List<String> matchedSkills,List<String> missingSkills,String explanation){}
 public record Response(String parserVersion,String modelVersion,List<String> parsingWarnings,
  List<Recommendation> recommendations){}
 public record Recommendation(String jobId,String jobTitle,String company,String location,String source,
  String jobUrl,double matchScore,double semanticScore,double skillScore,double roleScore,
  List<String> matchedSkills,List<String> missingSkills,String explanation){}
}
