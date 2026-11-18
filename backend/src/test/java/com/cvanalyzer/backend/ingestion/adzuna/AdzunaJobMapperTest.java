package com.cvanalyzer.backend.ingestion.adzuna;

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

class AdzunaJobMapperTest {
 @Test void mapsProviderFieldsAndClassification(){
  Clock clock=Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"),ZoneOffset.UTC);
  var mapper=new AdzunaJobMapper(clock,new ItJobClassifier(clock));
  var source=new AdzunaResponse.AdzunaJob("42","Developpeur logiciel","Build APIs",
   Instant.parse("2026-10-20T10:00:00Z"),"https://example.test/job",
   new AdzunaResponse.Company("Acme"),new AdzunaResponse.Location("Paris",List.of("France","Paris")),
   new AdzunaResponse.Category("IT Jobs","it-jobs"),"permanent","full_time");
  var job=mapper.map(source);
  assertThat(job.source()).isEqualTo(JobSource.ADZUNA);
  assertThat(job.location().countryCode()).isEqualTo("France");
  assertThat(job.employmentTypes()).containsExactly("permanent","full_time");
  assertThat(job.itClassification().isItRelevant()).isTrue();
  assertThat(job.sourceMetadata().categories()).containsExactly("IT Jobs","it-jobs");
 }
 @Test void rejectsInvalidProviderRecord(){
  Clock clock=Clock.systemUTC();
  var mapper=new AdzunaJobMapper(clock,new ItJobClassifier(clock));
  var source=new AdzunaResponse.AdzunaJob(null,"Engineer",null,null,
   "https://example.test/job",null,null,null,null,null);
  assertThatThrownBy(()->mapper.map(source)).isInstanceOf(ExternalJobValidationException.class)
   .hasMessageContaining("id");
 }
}
