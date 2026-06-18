package com.hana.exchange.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hana.omnilens.api")
public record ExchangeBackendProperties(
		String baseUrl,
		String apiKey
) {
}
