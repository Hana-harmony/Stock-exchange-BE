package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.rate-limit")
public record RateLimitProperties(
		boolean enabled,
		int maxRequests,
		Duration window
) {
	public RateLimitProperties {
		if (maxRequests <= 0) {
			maxRequests = 120;
		}
		if (window == null || window.isNegative() || window.isZero()) {
			window = Duration.ofMinutes(1);
		}
	}
}
