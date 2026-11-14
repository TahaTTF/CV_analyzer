package com.cvanalyzer.backend.cv;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cvanalyzer.backend.dto.CvParseResponse;

@Service
public class CvParsingService {
 public static final String VERSION="cv-parser-1.3";
 private final CvDocumentExtractionService documents;
 private final CandidateProfileExtractor profiles;
 private final SemanticProfileComposer semantic;
 public CvParsingService(CvDocumentExtractionService documents,CandidateProfileExtractor profiles,
   SemanticProfileComposer semantic){this.documents=documents;this.profiles=profiles;this.semantic=semantic;}
 public CvParseResponse parse(MultipartFile file){
  ExtractedCvDocument document=documents.extract(file);
  CandidateProfile profile=profiles.extract(document.text());
  NlpTextRepresentation text=semantic.compose(profile);
  List<String> warnings=warnings(profile,text);
  return new CvParseResponse("SUCCESS",VERSION,map(profile),text.professionalSummaryText(),text.skillsText(),
   text.experienceText(),text.projectsText(),text.educationText(),text.semanticProfileText(),warnings);
 }
 private CvParseResponse.Profile map(CandidateProfile value){return new CvParseResponse.Profile(
  value.targetRole(),value.professionalSummary(),value.technicalSkills(),value.yearsOfExperience(),
  value.experience().stream().map(v->new CvParseResponse.Experience(v.role(),v.organization(),v.period(),
   v.description())).toList(),value.projects().stream().map(v->new CvParseResponse.Project(v.name(),
   v.description(),v.technologies())).toList(),value.education().stream().map(v->new CvParseResponse.Education(
   v.qualification(),v.institution(),v.period(),v.description())).toList(),value.languages());}
 private List<String> warnings(CandidateProfile profile,NlpTextRepresentation text){List<String> values=new ArrayList<>();
  if(profile.targetRole()==null)values.add("TARGET_ROLE_NOT_FOUND");
  if(profile.professionalSummary()==null)values.add("PROFESSIONAL_SUMMARY_NOT_FOUND");
  if(profile.technicalSkills().isEmpty())values.add("TECHNICAL_SKILLS_NOT_FOUND");
  if(profile.experience().isEmpty())values.add("WORK_EXPERIENCE_NOT_FOUND");
  if(profile.education().isEmpty())values.add("EDUCATION_NOT_FOUND");
  if(profile.languages().isEmpty())values.add("LANGUAGES_NOT_FOUND");
  if(profile.yearsOfExperience()==null)values.add("YEARS_OF_EXPERIENCE_NOT_RELIABLY_AVAILABLE");
  if(text.semanticProfileText()==null)values.add("SEMANTIC_PROFILE_EMPTY");
  return List.copyOf(values);}
}
