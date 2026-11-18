package com.cvanalyzer.backend.enrichment;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.model.*;

class LocationNormalizationServiceTest {
 private final LocationNormalizationService service=new LocationNormalizationService();
 @Test void usesReliablePostalCountryAndHybridEvidence(){
  var result=service.normalize(job(new JobDocument.Location("75001 Paris",null,"FR",false)),
   "Organisation hybride avec teletravail partiel");
  assertThat(result.city()).isEqualTo("Paris");
  assertThat(result.countryCode()).isEqualTo("FR");
  assertThat(result.workMode()).isEqualTo(JobDocument.WorkMode.HYBRID);
  assertThat(result.original()).isEqualTo("75001 Paris");
 }
 @Test void respectsExplicitRemoteAndOnSiteSignals(){
  assertThat(service.normalize(job(new JobDocument.Location("Germany",null,"DE",true)),"")
   .workMode()).isEqualTo(JobDocument.WorkMode.REMOTE);
  assertThat(service.normalize(job(new JobDocument.Location("Berlin, Deutschland",null,null,false)),
   "Arbeit vor Ort").workMode()).isEqualTo(JobDocument.WorkMode.ON_SITE);
 }
 @Test void leavesMissingAndAmbiguousLocationsUnknown(){
  var result=service.normalize(job(null),"No location information");
  assertThat(result.city()).isNull();assertThat(result.countryCode()).isNull();
  assertThat(result.workMode()).isEqualTo(JobDocument.WorkMode.UNKNOWN);
  assertThat(result.confidence()).isZero();
 }
 @Test void retainsReliableCityWithoutGuessingCountry(){
  var result=service.normalize(job(new JobDocument.Location("Berlin",null,null,false)),"");
  assertThat(result.city()).isEqualTo("Berlin");assertThat(result.countryCode()).isNull();
  assertThat(result.confidence()).isEqualTo(0.6);
 }
 @Test void normalizesProviderCountryNamesToIsoCodes(){
  var result=service.normalize(job(new JobDocument.Location("Paris, Ile-de-France",null,"France",false)),"");
  assertThat(result.countryCode()).isEqualTo("FR");assertThat(result.city()).isEqualTo("Paris");
 }
 private JobDocument job(JobDocument.Location location){
  Instant now=Instant.parse("2026-10-21T10:00:00Z");
  return new JobDocument("id",JobSource.ARBEITNOW,"ext","Engineer",new JobDocument.Company("Acme"),
   "",location,new JobDocument.Requirements(List.of()),List.of(),"https://example.test/job",
   now,now,now,JobStatus.ACTIVE,null,null);
 }
}
