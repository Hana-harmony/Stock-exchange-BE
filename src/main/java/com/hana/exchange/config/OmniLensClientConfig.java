package com.hana.exchange.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OmniLensClientConfig {

	@Bean
	RestClient omniLensRestClient(RestClient.Builder builder, ExchangeBackendProperties properties) {
		return builder.baseUrl(properties.baseUrl()).build();
	}
}
