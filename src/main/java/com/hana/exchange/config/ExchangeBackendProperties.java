package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hana.omnilens.api")
public record ExchangeBackendProperties(
		String baseUrl,
		String apiKey,
		Duration quoteCacheTtl,
		Duration quoteCacheStaleTtl
) {
	public ExchangeBackendProperties {
		if (quoteCacheTtl == null) {
			quoteCacheTtl = Duration.ofSeconds(3);
		}
		if (quoteCacheStaleTtl == null) {
			quoteCacheStaleTtl = Duration.ofSeconds(30);
		}
	}
}
