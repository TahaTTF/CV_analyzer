package com.cvanalyzer.backend.cv;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.enrichment.TechnicalSkillExtractor;

class CandidateProfileExtractorTest {
 private final CandidateProfileExtractor extractor=new CandidateProfileExtractor(
  new TechnicalSkillExtractor(),new PersonalDataRedactor());

 @Test void extractsEnglishProfileConservatively(){
  String text="John Doe\njohn@example.test\n+33 6 12 34 56 78\nBackend Developer\n\n"
   +"Professional Summary\nBackend developer with 5 years of experience building REST APIs.\n\n"
   +"Technical Skills\nJava, Spring Boot, JavaScript, MongoDB, Docker, Git\n\n"
   +"Work Experience\nBackend Developer | Example Corp\n2021 - Present\nBuilt REST APIs using Java and Spring Boot.\n\n"
   +"Education\nMaster of Computer Science\nExample University\n2018 - 2020\n\n"
   +"Languages\nEnglish - Fluent, French - B2";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.targetRole()).isEqualTo("Backend Developer");
  assertThat(result.technicalSkills()).contains("Java","Spring Boot","JavaScript","MongoDB","Docker","Git");
  assertThat(result.yearsOfExperience()).isEqualTo(5);
  assertThat(result.experience()).singleElement().satisfies(v->{assertThat(v.role()).isEqualTo("Backend Developer");
   assertThat(v.organization()).isEqualTo("Example Corp");assertThat(v.description()).contains("REST APIs");});
  assertThat(result.education()).singleElement().satisfies(v->assertThat(v.qualification())
   .isEqualTo("Master of Computer Science"));
  assertThat(result.languages()).containsExactly("English","French");
 }
 @Test void supportsFrenchSectionsAndDoesNotUseEducationYearsAsExperience(){
  String text="Developpeuse Java\nProfil\nDeveloppeuse backend.\nCompetences techniques\nJava; Spring Boot; PostgreSQL\n"
   +"Experience professionnelle\nDeveloppeuse | Societe Exemple\n2023 - 2025\nServices Java.\n"
   +"Formation\nMaster Informatique\nUniversite\n2018 - 2020\nLangues\nFrancais, Anglais";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.targetRole()).isEqualTo("Developpeuse Java");
  assertThat(result.technicalSkills()).contains("Java","Spring Boot","PostgreSQL");
  assertThat(result.yearsOfExperience()).isNull();
  assertThat(result.education()).isNotEmpty();assertThat(result.languages()).contains("French","English");
 }
 @Test void returnsMissingFieldsInsteadOfInventingThem(){
  CandidateProfile result=extractor.extract("John Doe\nHobbies\nReading and travel");
  assertThat(result.targetRole()).isNull();assertThat(result.professionalSummary()).isNull();
  assertThat(result.technicalSkills()).isEmpty();assertThat(result.experience()).isEmpty();
  assertThat(result.education()).isEmpty();assertThat(result.yearsOfExperience()).isNull();
 }
 @Test void doesNotTurnGenericBusinessTermsIntoAnItProfile(){
  CandidateProfile result=extractor.extract("Marketing Manager\nProfessional Summary\n"
   +"Responsible for digital strategy, sales management, data reporting and customer communication.\n"
   +"Skills\nManagement, sales, communication");
  assertThat(result.targetRole()).isNull();
  assertThat(result.technicalSkills()).isEmpty();
  assertThat(result.yearsOfExperience()).isNull();
 }
 @Test void parsesContactHeaderMultipleEntriesAndProjectsWithoutBlankSeparators(){
  String text="Candidate Name\ncandidate@example.test\n+33 6 00 00 00 00\n"
   +"Second-year Engineering Student specializing in Data Engineering - Full-Stack Developer\n"
   +"Experience\nFull Stack Developer, Example Consulting\nAugust - September 2026\n"
   +"Built REST APIs with Maven and tested them with Postman.\n"
   +"Front-End Developer, Example Association\nJune - July 2024\nBuilt an Android user interface.\n"
   +"Personal Projects\nVision Tool\nComputer vision object detection platform.\n"
   +"Technologies: YOLOv8, OpenCV\nData Integration Suite\n- Built ETL and analytics workflows.\n"
   +"Technologies: SSIS, SSAS\nX-O Tournament\nDeveloped a web tournament application.\n"
   +"Technologies: GitHub, REST APIs\nMedia Data Warehouse & Analytics Platform\n"
   +"Designed analytical data pipelines.\nTechnologies: SSIS, SSAS\nCommunity Mobile App\n"
   +"Created an Android application.\n- Technologies: Android\n"
   +"Education\nNational Engineering Diploma in Data Engineering\nExample Institute\n"
   +"September 2025 - Present\nIntegrated Preparatory Program in Computer Science\n"
   +"Example Academy\nSeptember 2023 - May 2025\nLanguages\nEnglish, French";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.targetRole()).isEqualTo(
   "Second-year Engineering Student specializing in Data Engineering - Full-Stack Developer");
  assertThat(result.experience()).hasSize(2);
  assertThat(result.experience()).extracting(CandidateProfile.ExperienceEntry::role)
   .containsExactly("Full Stack Developer","Front-End Developer");
  assertThat(result.experience()).allSatisfy(entry->assertThat(entry.description())
   .doesNotContain("Vision Tool","Data Integration Suite"));
  assertThat(result.education()).hasSize(2);
  assertThat(result.education()).extracting(CandidateProfile.EducationEntry::qualification)
   .containsExactly("National Engineering Diploma in Data Engineering",
    "Integrated Preparatory Program in Computer Science");
  assertThat(result.projects()).hasSize(5);
  assertThat(result.projects()).extracting(CandidateProfile.ProjectEntry::name)
   .containsExactly("Vision Tool","Data Integration Suite","X-O Tournament",
    "Media Data Warehouse & Analytics Platform","Community Mobile App");
  assertThat(result.projects().getFirst().technologies()).contains("OpenCV","YOLOv8");
  assertThat(result.technicalSkills()).contains("YOLOv8","OpenCV","SSIS","SSAS","Maven",
   "Postman","Android","GitHub","REST APIs");
 }
 @Test void separatesFrenchEducationFromCertificationsAndExtracurricularActivities(){
  String text="Étudiante en ingénierie logicielle\nProfil\nDéveloppeuse orientée services backend.\n"
   +"Formation académique\nDiplôme national d'ingénieur\nInstitut Exemple\n2024 - Présent\n"
   +"Cycle préparatoire en informatique\nUniversité Exemple\n2022 - 2024\n"
   +"Expérience extra-scolaire\nResponsable d'un club étudiant\n2023 - 2024\n"
   +"Certifications\nCertification cloud fondamentale\nBénévolat\nMentorat associatif\n"
   +"Langues\nFrançais - courant; Anglais - intermédiaire\nStage de découverte en entreprise";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.education()).hasSize(2);
  assertThat(result.education()).extracting(CandidateProfile.EducationEntry::qualification)
   .containsExactly("Diplôme national d'ingénieur","Cycle préparatoire en informatique");
  assertThat(result.experience()).isEmpty();
  assertThat(result.languages()).containsExactly("French","English");
  assertThat(result.languages()).noneMatch(v->v.contains("Stage"));
  assertThat(result.additionalSections()).extracting(CandidateProfile.AdditionalSection::heading)
   .containsExactly("Certifications","Volunteering","Extracurricular activities");
 }
 @Test void ignoresProfileLinksAsExperienceAndKeepsAmbiguousFieldsNull(){
  String text="Software Engineering Student\nExperience\nhttps://portfolio.example/profile\n"
   +"github.com/example\nStages\nSoftware Intern\nExample Company\nJune 2025 - August 2025\n"
   +"Implemented tested services.\nEducation\nStudent association member\nLanguages\nEnglish";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.experience()).singleElement().satisfies(entry->{
   assertThat(entry.role()).isEqualTo("Software Intern");
   assertThat(entry.organization()).isEqualTo("Example Company");
   assertThat(entry.period()).isEqualTo("June 2025 - August 2025");
   assertThat(entry.description()).doesNotContain("portfolio.example","github.com");
  });
  assertThat(result.education()).isEmpty();
  assertThat(result.languages()).containsExactly("English");
 }
 @Test void recognizesDecoratedCompoundHeadingsBareYearsAndEmbeddedProjects(){
  String text="Data Engineering Student\n◆ Profile\nBuilds reliable distributed data services.\n"
   +"◆ Professional Experience\nCloud Engineering Intern • Example Company\n2026\nBuilt deployment automation.\n"
   +"Research Engineer • Example Laboratory\n2025\nBuilt streaming pipelines.\n"
   +"Personal Project — Fraud Detection Platform\nImplemented a Kafka scoring pipeline.\n"
   +"Academic Project — Data Warehouse\nImplemented SSIS packages.\n"
   +"◆ Education\nData Engineering • Example University\n2025 - Present\n"
   +"◆ Leadership & Certifications\nCloud certification\n"
   +"◆ Languages & Interests\nEnglish - Fluent; French - B2; Arabic - Native; hiking";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.experience()).hasSize(2);
  assertThat(result.experience()).extracting(CandidateProfile.ExperienceEntry::period)
   .containsExactly("2026","2025");
  assertThat(result.projects()).extracting(CandidateProfile.ProjectEntry::name)
   .containsExactly("Fraud Detection Platform","Data Warehouse");
  assertThat(result.education()).singleElement().satisfies(entry->{
   assertThat(entry.qualification()).isEqualTo("Data Engineering");
   assertThat(entry.institution()).isEqualTo("Example University");
  });
  assertThat(result.languages()).containsExactly("English","French","Arabic");
  assertThat(result.additionalSections()).extracting(CandidateProfile.AdditionalSection::heading)
   .contains("Certifications");
 }
 @Test void recoversExplicitProjectEvidenceFromAMislabeledExperienceSection(){
  String text="Engineering Student\nExperience\nCluster Platform (Docker)\n03/2026 - 04/2026\n"
   +"Built a distributed processing cluster.\nMobile Developer Internship\nExample Company\n"
   +"06/2025 - 08/2025\nDeveloped an Android application.\nEducation\nEngineering Degree\n"
   +"Example University\n2024 - Present";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.experience()).singleElement().satisfies(entry->
   assertThat(entry.role()).isEqualTo("Mobile Developer Internship"));
  assertThat(result.projects()).singleElement().satisfies(project->{
   assertThat(project.name()).isEqualTo("Cluster Platform (Docker)");
   assertThat(project.technologies()).contains("Docker");
  });
 }
 @Test void recognizesCompactHeadingsAndSeparatesConsecutiveUndatedRoles(){
  String text="Engineering Student\nEXPÉRIENCEPROFESSIONNELLE\nBackend Developer • Example One\n"
   +"Built APIs.\nData Engineer • Example Two\nBuilt pipelines.\nFORMATIONACADÉMIQUE\n"
   +"DiplômeNationalDIngénieur\n2023 - 2026\nLangues : Français, Anglais";
  CandidateProfile result=extractor.extract(text);
  assertThat(result.experience()).hasSize(2);
  assertThat(result.experience()).extracting(CandidateProfile.ExperienceEntry::role)
   .containsExactly("Backend Developer","Data Engineer");
  assertThat(result.education()).singleElement().satisfies(entry->
   assertThat(entry.qualification()).isEqualTo("DiplômeNationalDIngénieur"));
  assertThat(result.languages()).containsExactly("French","English");
 }
}
