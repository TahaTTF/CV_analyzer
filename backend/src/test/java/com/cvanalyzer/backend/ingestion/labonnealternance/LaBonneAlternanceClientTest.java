package com.cvanalyzer.backend.ingestion.labonnealternance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.cvanalyzer.backend.config.LaBonneAlternanceProperties;
import com.cvanalyzer.backend.exception.ProviderConfigurationException;
import com.cvanalyzer.backend.exception.ExternalServiceException;

class LaBonneAlternanceClientTest {
 @Test void sendsBearerAndOfficialRomeQuery(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("https://example.test/api/job/v1/search?romes=M1805,M1806"))
   .andExpect(method(org.springframework.http.HttpMethod.GET))
   .andExpect(header(HttpHeaders.AUTHORIZATION,"Bearer secret"))
   .andRespond(withSuccess("{\"jobs\":[],\"recruiters\":[],\"warnings\":[]}",MediaType.APPLICATION_JSON));
  var props=new LaBonneAlternanceProperties(URI.create("https://example.test/api"),"secret",
   "M1805,M1806",1,Duration.ofSeconds(1),Duration.ofSeconds(1));
  assertThat(new LaBonneAlternanceClient(builder.build(),props).search().jobs()).isEmpty();
  server.verify();
 }
 @Test void rejectsMissingApiKeyBeforeRequest(){
  var props=properties("");
  var client=new LaBonneAlternanceClient(RestClient.builder().baseUrl("https://example.test").build(),props);
  assertThatThrownBy(client::search).isInstanceOf(ProviderConfigurationException.class);
 }
 @Test void translatesQuotaResponse(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(anything()).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withTooManyRequests());
  assertThatThrownBy(()->new LaBonneAlternanceClient(builder.build(),properties("secret")).search())
   .isInstanceOf(ExternalServiceException.class);
  server.verify();
 }
 private LaBonneAlternanceProperties properties(String key){
  return new LaBonneAlternanceProperties(URI.create("https://example.test/api"),key,
   "M1805,M1806",1,Duration.ofSeconds(1),Duration.ofSeconds(1));
 }
}
