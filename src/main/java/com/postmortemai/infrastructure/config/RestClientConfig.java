package com.postmortemai.infrastructure.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration for the Spring RestClient, configuring the timeouts mapped from properties.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(OpenAiProperties properties, RestClient.Builder builder) {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = 
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}
