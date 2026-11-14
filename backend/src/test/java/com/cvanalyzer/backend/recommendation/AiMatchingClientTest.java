package com.cvanalyzer.backend.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import com.cvanalyzer.backend.config.AiMatchingProperties;
import com.cvanalyzer.backend.dto.RecommendationDtos;
import com.sun.net.httpserver.HttpServer;

class AiMatchingClientTest {
    @Test
    void usesHttp11AndMapsFastApiResponse() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> protocol = new AtomicReference<>();
        server.createContext("/match", exchange -> {
            protocol.set(exchange.getProtocol());
            exchange.getRequestBody().readAllBytes();
            byte[] body = ("{\"modelVersion\":\"test-model\",\"recommendations\":[{" +
                    "\"jobId\":\"job-1\",\"matchScore\":88.0,\"semanticScore\":90.0," +
                    "\"skillScore\":80.0,\"roleScore\":70.0,\"matchedSkills\":[\"python\"]," +
                    "\"missingSkills\":[],\"explanation\":\"Matched skills: python.\"}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            AiMatchingClient client = new AiMatchingClient(new AiMatchingProperties(base, Duration.ofSeconds(1),
                    Duration.ofSeconds(2)));
            var request = new RecommendationDtos.AiMatchRequest("sanitized profile", List.of("Python"),
                    "Data Engineer", List.of(new RecommendationDtos.AiJobCandidate("job-1", "Data Engineer",
                            "Python", List.of("Python"), List.of("data"))), 1);

            var response = client.match(request);

            assertThat(protocol).hasValue("HTTP/1.1");
            assertThat(response.modelVersion()).isEqualTo("test-model");
            assertThat(response.recommendations()).singleElement().satisfies(value ->
                    assertThat(value.jobId()).isEqualTo("job-1"));
        } finally {
            server.stop(0);
        }
    }
}
