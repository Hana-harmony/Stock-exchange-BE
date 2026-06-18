package com.hana.exchange.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "exchange.notification.push")
public record NotificationPushProperties(
		boolean workerEnabled,
		int batchSize,
		int maxAttemptCount,
		Duration fixedDelay,
		List<String> enabledProviders
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
		if (enabledProviders == null || enabledProviders.isEmpty()) {
			enabledProviders = List.of("LOCAL_NOOP_PUSH");
		}
		enabledProviders = enabledProviders.stream()
				.filter(provider -> provider != null && !provider.isBlank())
				.map(String::trim)
				.toList();
	}
}
