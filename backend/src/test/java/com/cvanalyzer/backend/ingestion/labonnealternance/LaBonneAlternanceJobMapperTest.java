package com.cvanalyzer.backend.ingestion.labonnealternance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.ingestion.ItJobClassifier;
import com.cvanalyzer.backend.model.JobSource;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;

class LaBonneAlternanceJobMapperTest {
 @Test void preservesProvenanceSkillsAndRomeCodes(){
  Clock clock=Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC);
  var mapper=new LaBonneAlternanceJobMapper(clock,new ItJobClassifier(clock));
  var source=new LaBonneAlternanceResponse.JobOffer(
   new LaBonneAlternanceResponse.Identifier("partner-7","lba-id","France Travail"),
   new LaBonneAlternanceResponse.Workplace("Acme",null,null,null,
    new LaBonneAlternanceResponse.Location("Paris",null),null,null,null,null),
   new LaBonneAlternanceResponse.Apply(null,"https://example.test/apply",null),
   new LaBonneAlternanceResponse.Contract(null,12,List.of("Apprentissage"),"hybrid",null,null),
   new LaBonneAlternanceResponse.Offer("Developpeur web",List.of("Java"),
    List.of("Spring Boot"),List.of(),1d,
    new LaBonneAlternanceResponse.Publication("2026-10-20T10:00:00Z",null),
    List.of("M1805"),"Build software",null,"Active",null),false);
  var job=mapper.map(source);
  assertThat(job.source()).isEqualTo(JobSource.LA_BONNE_ALTERNANCE);
  assertThat(job.externalId()).isEqualTo("France Travail:partner-7");
  assertThat(job.sourceMetadata().partnerLabel()).isEqualTo("France Travail");
  assertThat(job.requirements().skills()).containsExactly("Java");
  assertThat(job.requirements().toBeAcquiredSkills()).containsExactly("Spring Boot");
  assertThat(job.location().remote()).isTrue();
  assertThat(job.itClassification().isItRelevant()).isTrue();
 }
 @Test void rejectsMissingProvenance(){
  Clock clock=Clock.systemUTC();
  var mapper=new LaBonneAlternanceJobMapper(clock,new ItJobClassifier(clock));
  var source=new LaBonneAlternanceResponse.JobOffer(null,null,null,null,null,false);
  assertThatThrownBy(()->mapper.map(source)).isInstanceOf(ExternalJobValidationException.class)
   .hasMessageContaining("identifier");
 }
}
