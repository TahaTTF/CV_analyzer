package com.cvanalyzer.backend.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
    @Bean("arbeitnowRestClient")
    RestClient arbeitnowRestClient(ArbeitnowProperties properties) {
        return client(properties.baseUrl(),properties.connectTimeout(),properties.readTimeout());
    }
    @Bean("adzunaRestClient")
    RestClient adzunaRestClient(AdzunaProperties properties) {
        return client(properties.baseUrl(),properties.connectTimeout(),properties.readTimeout());
    }
    @Bean("laBonneAlternanceRestClient")
    RestClient laBonneAlternanceRestClient(LaBonneAlternanceProperties properties) {
        return client(properties.baseUrl(),properties.connectTimeout(),properties.readTimeout());
    }
    private RestClient client(java.net.URI baseUrl,java.time.Duration connectTimeout,
            java.time.Duration readTimeout){
        HttpClient client = HttpClient.newBuilder().connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl.toString()).requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "cv-analyzer-university-project/0.1").build();
    }
}
