package com.cvanalyzer.backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ItJobClassifierTest {
 private static final List<String> ADZUNA_IT_CATEGORY=List.of("Emplois Informatique","it-jobs");
 private final ItJobClassifier classifier=new ItJobClassifier(Clock.fixed(
  Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC));

 @Test void preservesRepresentativeEnglishFrenchAndGermanItRoles(){
  assertRelevant("Senior Software Engineer","");
  assertRelevant("Data Scientist","");
  assertRelevant("DevOps Engineer","");
  assertRelevant("Network Engineer","");
  assertRelevant("D\u00e9veloppeur Java / Angular","");
  assertRelevant("Softwareentwickler Cloud","");
  assertRelevant("Senior Machine Learning Engineering Manager","");
  assertRelevant("Ing\u00e9nieur logiciel full stacks & IoT H/F","");
  assertRelevant("Consultant fonctionnel SAP","");
  assertRelevant("DBA PostgreSQL","");
  assertRelevant("Tech Lead Java / Spark","");
  assertRelevant("Lead Tech node.js & IA","");
  assertRelevant("Business analyste informatique Senior","");
  assertRelevant("Ing\u00e9nieur Qualit\u00e9 IA","");
 }

 @Test void rejectsKnownFalsePositivesDespiteAdzunaItCategory(){
  assertNotRelevant("Livreur de Journaux Distributeur de Journaux H/F",ADZUNA_IT_CATEGORY);
  assertNotRelevant("Conseiller Multimarques - Aurillac H/F",ADZUNA_IT_CATEGORY);
  assertNotRelevant("Expert en Gestion d'Entreprise H/F",ADZUNA_IT_CATEGORY);
  assertNotRelevant("Ing\u00e9nieur Charg\u00e9 d'Affaires en Radioprotection H/F",ADZUNA_IT_CATEGORY);
  assertNotRelevant("Charg\u00e9 d'\u00c9tudes en \u00c9lectricit\u00e9 Tertiaire H/F",ADZUNA_IT_CATEGORY);
 }

 @Test void broadProviderCategoryAloneIsAmbiguous(){
  var result=classifier.classify("Consultant", "",ADZUNA_IT_CATEGORY,List.of());
  assertThat(result.isItRelevant()).isNull();
  assertThat(result.matchedCategories()).contains("it-category");
 }

 @Test void genericWordsDoNotAutomaticallyQualifyJobs(){
  assertThat(classifier.classify("Mechanical Engineer","",List.of(),List.of()).isItRelevant()).isFalse();
  assertThat(classifier.classify("Management Consultant","",List.of("management"),List.of()).isItRelevant()).isFalse();
  assertThat(classifier.classify("Digital Marketing Manager","",List.of("digital"),List.of()).isItRelevant()).isFalse();
  assertThat(classifier.classify("Data Entry Clerk","",List.of("data"),List.of()).isItRelevant()).isFalse();
  assertThat(classifier.classify("Building Systems Engineer","",List.of("system"),List.of()).isItRelevant()).isFalse();
 }

 @Test void strongItTitleWinsWhenDescriptionMentionsAnotherIndustry(){
  assertRelevant("Software Engineer","Builds claims software for insurance sales and finance teams");
  assertRelevant("D\u00e9veloppeuse Java","Applications pour des clients du secteur de la sant\u00e9");
 }

 @Test void corroboratedTechnicalContextCanQualifyAGenericTitle(){
  var result=classifier.classify("Technical Specialist",
   "Backend application development with Java, Spring Boot and Docker",List.of(),List.of());
  assertThat(result.isItRelevant()).isTrue();
  assertThat(result.matchedCategories()).contains("technical-skills","technical-context");
 }

 @Test void aSingleDescriptionHintRemainsAmbiguous(){
  var result=classifier.classify("Project Manager","Coordinates a team using Java",List.of(),List.of());
  assertThat(result.isItRelevant()).isNull();
  assertThat(result.relevanceReason()).contains("too weak");
 }

 @Test void authoritativeRomeEvidenceStillQualifiesUnlessTitleIsExplicitlyNonIt(){
  assertThat(classifier.classify("Consultant","",List.of(),List.of("M1805")).isItRelevant()).isTrue();
  assertThat(classifier.classify("Infirmi\u00e8re","",List.of(),List.of("M1805")).isItRelevant()).isFalse();
 }

 @Test void avoidsNaiveSubstrings(){
  assertThat(classifier.classify("Salesforce Account Manager","",List.of(),List.of()).isItRelevant()).isFalse();
  assertThat(classifier.classify("Business Developer","",List.of(),List.of()).isItRelevant()).isFalse();
 }

 private void assertRelevant(String title,String description){
  assertThat(classifier.classify(title,description,List.of(),List.of()).isItRelevant()).isTrue();
 }
 private void assertNotRelevant(String title,List<String> structured){
  assertThat(classifier.classify(title,"",structured,List.of()).isItRelevant()).isFalse();
 }
}
