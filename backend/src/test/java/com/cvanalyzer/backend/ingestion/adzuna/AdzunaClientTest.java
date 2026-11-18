package com.cvanalyzer.backend.ingestion.adzuna;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.cvanalyzer.backend.config.AdzunaProperties;
import com.cvanalyzer.backend.exception.ProviderConfigurationException;
import com.cvanalyzer.backend.exception.ExternalServiceException;

class AdzunaClientTest {
 @Test void sendsOfficialSearchShapeAndParsesResponse(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/v1/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("https://example.test/v1/api/jobs/fr/search/1?app_id=id&app_key=key&results_per_page=50&category=it-jobs&content-type=application/json"))
   .andExpect(method(org.springframework.http.HttpMethod.GET))
   .andRespond(withSuccess("{\"count\":1,\"results\":[{\"id\":\"42\",\"title\":\"Software Engineer\",\"created\":\"2026-10-20T10:00:00Z\"}]}",
    MediaType.APPLICATION_JSON));
  var response=new AdzunaClient(builder.build(),properties("id","key")).fetchPage(1);
  assertThat(response.results()).singleElement().extracting(AdzunaResponse.AdzunaJob::id).isEqualTo("42");
  server.verify();
 }
 @Test void failsBeforeRequestWhenCredentialsMissing(){
  var client=new AdzunaClient(RestClient.builder().baseUrl("https://example.test").build(),properties("",""));
  assertThatThrownBy(()->client.fetchPage(1)).isInstanceOf(ProviderConfigurationException.class);
 }
 @Test void translatesQuotaResponse(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/v1/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(anything()).andRespond(withTooManyRequests());
  assertThatThrownBy(()->new AdzunaClient(builder.build(),properties("id","key")).fetchPage(1))
   .isInstanceOf(ExternalServiceException.class);
  server.verify();
 }
 private AdzunaProperties properties(String id,String key){
  return new AdzunaProperties(URI.create("https://example.test/v1/api"),id,key,"fr","",
   50,1,1,Duration.ofSeconds(1),Duration.ofSeconds(1));
 }
}
