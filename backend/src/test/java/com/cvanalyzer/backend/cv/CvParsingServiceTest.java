package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CvParsingServiceTest {
 @Test void mapsProfileAndWarningsWithoutPersistence(){
  var documents=mock(CvDocumentExtractionService.class);var profiles=mock(CandidateProfileExtractor.class);
  var semantic=mock(SemanticProfileComposer.class);var file=new MockMultipartFile("file","cv.pdf","application/pdf",new byte[]{1});
  var profile=new CandidateProfile("Backend Developer",null,List.of("Java"),null,List.of(),List.of(),List.of(),List.of(),List.of());
  when(documents.extract(file)).thenReturn(new ExtractedCvDocument(CvDocumentType.PDF,"text",1));
  when(profiles.extract("text")).thenReturn(profile);
  when(semantic.compose(profile)).thenReturn(new NlpTextRepresentation(null,"Technical skills: Java.",null,null,null,
   "Backend Developer. Technical skills: Java."));
  var result=new CvParsingService(documents,profiles,semantic).parse(file);
  assertThat(result.parsingStatus()).isEqualTo("SUCCESS");assertThat(result.parserVersion()).isEqualTo("cv-parser-1.3");
  assertThat(result.profile().technicalSkills()).containsExactly("Java");
  assertThat(result.warnings()).contains("PROFESSIONAL_SUMMARY_NOT_FOUND","WORK_EXPERIENCE_NOT_FOUND",
   "YEARS_OF_EXPERIENCE_NOT_RELIABLY_AVAILABLE");
 }
}
