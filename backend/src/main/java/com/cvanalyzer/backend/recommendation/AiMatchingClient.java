package com.cvanalyzer.backend.recommendation;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.cvanalyzer.backend.config.AiMatchingProperties;
import com.cvanalyzer.backend.dto.RecommendationDtos.AiMatchRequest;
import com.cvanalyzer.backend.dto.RecommendationDtos.AiMatchResponse;
import com.cvanalyzer.backend.exception.RecommendationException;
import org.springframework.http.HttpStatus;

@Component
public class AiMatchingClient {
    private final RestClient client;

    public AiMatchingClient(AiMatchingProperties properties) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(properties.readTimeout());
        client = RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(factory)
                .build();
    }

    public AiMatchResponse match(AiMatchRequest request) {
        try {
            AiMatchResponse response = client.post()
                    .uri("/match")
                    .body(request)
                    .retrieve()
                    .body(AiMatchResponse.class);
            if (response == null || response.recommendations() == null) {
                throw new RecommendationException(HttpStatus.BAD_GATEWAY,
                        "AI_RESPONSE_INVALID", "AI matching service returned an invalid response");
            }
            return response;
        } catch (RecommendationException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            boolean timeout = hasCause(ex, HttpTimeoutException.class);
            throw new RecommendationException(timeout ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE,
                    timeout ? "AI_SERVICE_TIMEOUT" : "AI_SERVICE_UNAVAILABLE",
                    timeout ? "AI matching service timed out" : "AI matching service is unavailable", ex);
        } catch (RestClientException ex) {
            throw new RecommendationException(HttpStatus.BAD_GATEWAY,
                    "AI_SERVICE_FAILURE", "AI matching service request failed", ex);
        }
    }

    private boolean hasCause(Throwable value, Class<? extends Throwable> type) {
        for (Throwable current = value; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }
}
