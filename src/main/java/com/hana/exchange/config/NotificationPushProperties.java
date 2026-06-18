package com.hana.exchange.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.notification.push")
public record NotificationPushProperties(
		boolean workerEnabled,
		int batchSize,
		int maxAttemptCount,
		Duration fixedDelay
) {
	public NotificationPushProperties {
		if (batchSize <= 0) {
			batchSize = 50;
		}
		if (maxAttemptCount <= 0) {
			maxAttemptCount = 3;
		}
		if (fixedDelay == null || fixedDelay.isNegative() || fixedDelay.isZero()) {
			fixedDelay = Duration.ofSeconds(30);
		}
	}
}
