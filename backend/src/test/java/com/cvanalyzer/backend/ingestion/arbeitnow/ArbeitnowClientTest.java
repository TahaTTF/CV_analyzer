package com.cvanalyzer.backend.ingestion.arbeitnow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.cvanalyzer.backend.exception.ExternalServiceException;

class ArbeitnowClientTest {
 @Test void parsesCurrentResponseAndPagination(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("https://example.test/api?page=2")).andRespond(withSuccess(
    """
    {"data":[{"slug":"job-1","company_name":"Example","title":"Engineer",
    "description":"<p>Work</p>","remote":true,"url":"https://example.test/job",
    "tags":["Tech"],"job_types":["Full Time"],"location":"Berlin","created_at":1700000000}],
    "links":{"next":"https://example.test/api?page=3"},"meta":{"current_page":2,"per_page":300}}
    """,MediaType.APPLICATION_JSON));
  ArbeitnowResponse response=new ArbeitnowClient(builder.build()).fetchPage(2);
  assertThat(response.data()).hasSize(1);
  assertThat(response.data().getFirst().companyName()).isEqualTo("Example");
  assertThat(response.links().next()).endsWith("page=3");
  server.verify();
 }

 @Test void translatesHttpFailure(){
  RestClient.Builder builder=RestClient.builder().baseUrl("https://example.test/api");
  MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  server.expect(requestTo("https://example.test/api?page=1"))
    .andRespond(withServerError());
  assertThatThrownBy(()->new ArbeitnowClient(builder.build()).fetchPage(1))
    .isInstanceOf(ExternalServiceException.class);
  server.verify();
 }
}
