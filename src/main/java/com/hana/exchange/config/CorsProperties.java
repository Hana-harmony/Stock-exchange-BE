package com.hana.exchange.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.cors")
public record CorsProperties(
		List<String> allowedOrigins,
		List<String> allowedMethods,
		List<String> allowedHeaders,
		boolean allowCredentials,
		Duration maxAge
) {
	public CorsProperties {
		if (allowedOrigins == null || allowedOrigins.isEmpty()) {
			allowedOrigins = List.of(
					"http://localhost:15100",
					"http://127.0.0.1:15100",
					"http://localhost:3000",
					"http://127.0.0.1:3000");
		}
		if (allowedMethods == null || allowedMethods.isEmpty()) {
			allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
		}
		if (allowedHeaders == null || allowedHeaders.isEmpty()) {
			allowedHeaders = List.of("Authorization", "Content-Type", "Accept", "X-Requested-With");
		}
		if (maxAge == null || maxAge.isNegative()) {
			maxAge = Duration.ofHours(1);
		}
	}
}
