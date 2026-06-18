package com.hana.exchange.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hana.omnilens.api")
public record ExchangeBackendProperties(
		String baseUrl,
		String apiKey,
		List<String> defaultStockCodes,
		Duration quoteCacheTtl,
		Duration quoteCacheStaleTtl
) {
	public ExchangeBackendProperties {
		if (defaultStockCodes == null || defaultStockCodes.isEmpty()) {
			defaultStockCodes = List.of("005930", "000660", "035420", "005380", "035720", "207940");
		}
		if (quoteCacheTtl == null) {
			quoteCacheTtl = Duration.ofSeconds(3);
		}
		if (quoteCacheStaleTtl == null) {
			quoteCacheStaleTtl = Duration.ofSeconds(30);
		}
	}
}
