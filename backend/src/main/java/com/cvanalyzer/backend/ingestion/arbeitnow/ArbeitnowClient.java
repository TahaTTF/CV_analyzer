package com.cvanalyzer.backend.ingestion.arbeitnow;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.cvanalyzer.backend.exception.ExternalServiceException;

@Component
public class ArbeitnowClient {
    private final RestClient client;
    public ArbeitnowClient(@Qualifier("arbeitnowRestClient") RestClient arbeitnowRestClient) {
        client = arbeitnowRestClient;
    }

    public ArbeitnowResponse fetchPage(int page) {
        try {
            var response = client.get().uri(b -> b.queryParam("page", page).build())
                    .retrieve().body(ArbeitnowResponse.class);
            if (response == null) {
                throw new ExternalServiceException("Arbeitnow returned an empty response", null);
            }
            return response;
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Arbeitnow request failed", ex);
        }
    }
}
