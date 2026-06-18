package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.auth")
public record AuthProperties(
		String tokenSigningKey,
		Duration accessTokenTtl,
		Duration refreshTokenTtl
) {
	public AuthProperties {
		if (tokenSigningKey == null || tokenSigningKey.isBlank()) {
			tokenSigningKey = "local-dev-token-signing-key-change-before-prod";
		}
		if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
			accessTokenTtl = Duration.ofHours(12);
		}
		if (refreshTokenTtl == null || refreshTokenTtl.isNegative() || refreshTokenTtl.isZero()) {
			refreshTokenTtl = Duration.ofDays(14);
		}
	}
}
