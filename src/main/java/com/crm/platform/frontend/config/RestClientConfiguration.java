package com.crm.platform.frontend.config;

import com.crm.platform.frontend.security.BearerTokenInterceptor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configures Spring 6 RestClient for the frontend with Bearer token injection and JSON mapping.
 */
@Configuration
public class RestClientConfiguration {

    private final FrontendProperties frontendProperties;

    public RestClientConfiguration(FrontendProperties frontendProperties) {
        this.frontendProperties = frontendProperties;
    }

    @Bean
    public ObjectMapper frontendObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    public RestClient backendRestClient(ObjectMapper frontendObjectMapper) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        return RestClient.builder()
                .baseUrl(frontendProperties.getApiBaseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new BearerTokenInterceptor())
                .messageConverters(converters -> converters.add(0, new MappingJackson2HttpMessageConverter(frontendObjectMapper)))
                .build();
    }
}
