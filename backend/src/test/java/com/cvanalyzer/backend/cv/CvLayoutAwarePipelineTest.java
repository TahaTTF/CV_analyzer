package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import com.cvanalyzer.backend.config.CvParserProperties;
import com.cvanalyzer.backend.enrichment.TechnicalSkillExtractor;

class CvLayoutAwarePipelineTest {
 @Test void parsesTwoColumnStudentCvByColumnAndSection() throws Exception {
  List<CvTestFixtures.PlacedText> page=List.of(
   text(50,765,14,"Data Engineering Student",Color.BLUE),
   text(50,725,13,"Profil",Color.BLUE),text(50,705,"Développeuse de services Java."),
   text(50,665,13,"Formation académique",Color.BLUE),
   text(50,645,"Diplôme d'ingénieur en données"),text(50,627,"Institut Exemple"),
   text(50,609,"2024 - Présent"),text(50,575,"Cycle préparatoire informatique"),
   text(50,557,"Université Exemple"),text(50,539,"2022 - 2024"),
   text(50,500,13,"Langues",Color.BLUE),text(50,482,"Français; Anglais"),
   text(330,725,13,"Stages",Color.RED),text(330,705,"Backend Developer Intern"),
   text(330,687,"Example Company"),text(330,669,"June 2025 - August 2025"),
   text(330,651,"Built Java REST APIs."),text(330,617,"Data Engineer Intern"),
   text(330,599,"Example Laboratory"),text(330,581,"June 2024 - July 2024"),
   text(330,563,"Created SQL pipelines."),text(330,520,13,"Projets académiques",Color.RED),
   text(330,500,"Analytics Platform"),text(330,482,"Built a data warehouse."),
   text(330,464,"Technologies: SQL, SSIS"),text(330,430,"Vision Service"),
   text(330,412,"Created object detection APIs."),text(330,394,"Technologies: OpenCV, YOLOv8"));
  byte[] bytes=CvTestFixtures.positionedPdf(List.of(page),true);
  CvDocumentExtractionService documents=new CvDocumentExtractionService(
   new CvParserProperties(5L*1024*1024,500_000,100),new CvTextNormalizationService());
  String extracted=documents.extract(new MockMultipartFile("file","layout.pdf","application/pdf",bytes)).text();
  CandidateProfile profile=new CandidateProfileExtractor(new TechnicalSkillExtractor(),
   new PersonalDataRedactor()).extract(extracted);
  assertThat(profile.professionalSummary()).isEqualTo("Développeuse de services Java.");
  assertThat(profile.education()).hasSize(2);
  assertThat(profile.experience()).hasSize(2);
  assertThat(profile.projects()).hasSize(2);
  assertThat(profile.languages()).containsExactly("French","English");
  assertThat(profile.experience()).extracting(CandidateProfile.ExperienceEntry::role)
   .containsExactly("Backend Developer Intern","Data Engineer Intern");
  assertThat(profile.projects()).extracting(CandidateProfile.ProjectEntry::name)
   .containsExactly("Analytics Platform","Vision Service");
  assertThat(profile.technicalSkills()).contains("Java","REST APIs","SQL","SSIS","OpenCV","YOLOv8");
 }
 private static CvTestFixtures.PlacedText text(float x,float y,String value){return new CvTestFixtures.PlacedText(x,y,value);}
 private static CvTestFixtures.PlacedText text(float x,float y,float size,String value,Color color){
  return new CvTestFixtures.PlacedText(x,y,size,value,color);}
}
