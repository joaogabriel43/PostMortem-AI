package com.postmortemai.infrastructure.config;

import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
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
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(properties.getConnectTimeout())
                .withReadTimeout(properties.getReadTimeout());

        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);

        return builder
                .requestFactory(requestFactory)
                .build();
    }
}
