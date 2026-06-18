package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.audit")
public record AuditProperties(
		int retentionDays,
		boolean retentionWorkerEnabled,
		Duration retentionFixedDelay
) {
	public AuditProperties {
		if (retentionDays <= 0) {
			retentionDays = 365;
		}
		if (retentionFixedDelay == null || retentionFixedDelay.isNegative() || retentionFixedDelay.isZero()) {
			retentionFixedDelay = Duration.ofHours(24);
		}
	}
}
