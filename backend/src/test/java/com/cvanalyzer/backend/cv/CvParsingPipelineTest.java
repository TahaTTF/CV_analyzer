package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;
import com.cvanalyzer.backend.config.CvParserProperties;
import com.cvanalyzer.backend.enrichment.TechnicalSkillExtractor;

class CvParsingPipelineTest {
 @ParameterizedTest @MethodSource("documents")
 void parsesPdfAndDocxEndToEndWithoutReturningPersonalIdentifiers(String name,String contentType,byte[] bytes)
   throws Exception {
  var redactor=new PersonalDataRedactor();
  var extraction=new CvDocumentExtractionService(new CvParserProperties(5L*1024*1024,500_000,100),
   new CvTextNormalizationService());
  var profiles=new CandidateProfileExtractor(new TechnicalSkillExtractor(),redactor);
  var service=new CvParsingService(extraction,profiles,new SemanticProfileComposer(redactor));
  var result=service.parse(new MockMultipartFile("file",name,contentType,bytes));
  assertThat(result.parsingStatus()).isEqualTo("SUCCESS");
  assertThat(result.profile().targetRole()).isEqualTo("Backend Developer");
  assertThat(result.profile().technicalSkills()).contains("Java","Spring Boot","Docker");
  assertThat(result.semanticProfileText()).contains("Backend Developer","Technical skills")
   .doesNotContain("john@example.test","+33 6 12 34 56 78","John Doe");
 }
 static Stream<Arguments> documents() throws Exception {
  String[] lines={"John Doe","john@example.test","+33 6 12 34 56 78","Backend Developer",
   "Professional Summary","Builds REST APIs with Java and Spring Boot.","Technical Skills",
   "Java, Spring Boot, Docker","Work Experience","Backend Developer | Example Corp",
   "2022 - Present","Built backend services.","Education","Computer Science Degree",
   "Example University","2018 - 2021","Languages","English, French"};
  return Stream.of(Arguments.of("candidate.pdf","application/pdf",CvTestFixtures.pdf(lines)),
   Arguments.of("candidate.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    CvTestFixtures.docx(lines)));
 }
}
