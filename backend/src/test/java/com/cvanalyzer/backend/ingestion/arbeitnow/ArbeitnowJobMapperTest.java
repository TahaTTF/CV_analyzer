package com.cvanalyzer.backend.ingestion.arbeitnow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.exception.ExternalJobValidationException;
import com.cvanalyzer.backend.model.JobSource;

class ArbeitnowJobMapperTest {
 private final ArbeitnowJobMapper mapper=new ArbeitnowJobMapper(
   Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"),ZoneOffset.UTC));

 @Test void mapsSourceFieldsWithoutInventingSkills(){
  var source=job("slug-1","Engineer");
  var result=mapper.map(source);
  assertThat(result.source()).isEqualTo(JobSource.ARBEITNOW);
  assertThat(result.externalId()).isEqualTo("slug-1");
  assertThat(result.company().name()).isEqualTo("Example");
  assertThat(result.location().remote()).isTrue();
  assertThat(result.requirements().skills()).isEmpty();
  assertThat(result.employmentTypes()).containsExactly("Full Time");
  assertThat(result.publishedAt()).isEqualTo(Instant.ofEpochSecond(1_700_000_000L));
 }

 @Test void rejectsMissingRequiredFields(){
  assertThatThrownBy(()->mapper.map(job(null,"Engineer")))
    .isInstanceOf(ExternalJobValidationException.class).hasMessageContaining("slug");
 }

 @Test void handlesOptionalFields(){
  var source=new ArbeitnowResponse.ArbeitnowJob("slug","Example","Engineer",null,null,
    "https://example.test/job",null,null,null,null);
  var result=mapper.map(source);
  assertThat(result.description()).isNull();
  assertThat(result.location().displayName()).isNull();
  assertThat(result.publishedAt()).isNull();
 }

 static ArbeitnowResponse.ArbeitnowJob job(String slug,String title){
  return new ArbeitnowResponse.ArbeitnowJob(slug,"Example",title,"<p>Build systems</p>",true,
    "https://example.test/job",List.of("Tech"),List.of("Full Time"),"Berlin",1_700_000_000L);
 }
}
