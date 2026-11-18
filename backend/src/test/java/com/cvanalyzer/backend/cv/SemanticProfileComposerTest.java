package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticProfileComposerTest {
 @Test void composesSupportedProfessionalTextWithoutContactIdentifiers(){
  var profile=new CandidateProfile("Backend Developer","Contact john@example.test or +33 6 12 34 56 78",
   List.of("Java","Spring Boot","Docker"),null,
   List.of(new CandidateProfile.ExperienceEntry("Backend Developer","Example Corp","2022 - Present",
    "Built REST APIs. https://portfolio.example/john")),List.of(),List.of(),List.of("English"),List.of());
  var result=new SemanticProfileComposer(new PersonalDataRedactor()).compose(profile);
  assertThat(result.skillsText()).isEqualTo("Technical skills: Java, Spring Boot, Docker.");
  assertThat(result.semanticProfileText()).contains("Backend Developer","Built REST APIs")
   .doesNotContain("john@example.test","+33","portfolio.example");
 }
 @Test void composesStructuredExperienceProjectsAndEducationWithoutMixingSections(){
  var profile=new CandidateProfile("Data Engineering Student - Full-Stack Developer",null,
   List.of("Java","REST APIs","YOLOv8","OpenCV"),null,
   List.of(new CandidateProfile.ExperienceEntry("Full Stack Developer","Example Company",
    "August - September 2026","Built REST APIs. candidate@example.test")),
   List.of(new CandidateProfile.ProjectEntry("Vision Tool","Implemented object detection.",
    List.of("YOLOv8","OpenCV"))),
   List.of(new CandidateProfile.EducationEntry("Engineering Diploma","Example Institute",
    "2025 - Present",null)),List.of(),List.of());
  var result=new SemanticProfileComposer(new PersonalDataRedactor()).compose(profile);
  assertThat(result.experienceText()).startsWith("Professional experience:")
   .contains("Full Stack Developer").doesNotContain("Vision Tool","candidate@example.test");
  assertThat(result.projectsText()).startsWith("Projects:").contains("Vision Tool","YOLOv8","OpenCV")
   .doesNotContain("Example Company","Engineering Diploma");
  assertThat(result.educationText()).startsWith("Education:").contains("Engineering Diploma")
   .doesNotContain("Vision Tool","Full Stack Developer");
  assertThat(result.semanticProfileText()).contains("Professional experience:","Projects:","Education:")
   .doesNotContain("candidate@example.test");
 }
}
