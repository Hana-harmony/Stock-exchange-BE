package com.hana.exchange.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hana.omnilens.api")
public record ExchangeBackendProperties(
		String baseUrl,
		String apiKey,
		List<String> defaultStockCodes
) {
	public ExchangeBackendProperties {
		if (defaultStockCodes == null || defaultStockCodes.isEmpty()) {
			defaultStockCodes = List.of("005930", "000660", "035420", "005380", "035720", "207940");
		}
	}
}
