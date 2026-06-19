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
		List<String> enabledProviders,
		TokenVault tokenVault,
		Fcm fcm
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
		if (tokenVault == null) {
			tokenVault = new TokenVault("");
		}
		if (fcm == null) {
			fcm = Fcm.defaults();
		}
	}

	public record TokenVault(String encryptionKey) {
		public boolean configured() {
			return encryptionKey != null && !encryptionKey.isBlank();
		}
	}

	public record Fcm(
			String projectId,
			String accessToken,
			String sendBaseUrl
	) {
		public Fcm {
			if (sendBaseUrl == null || sendBaseUrl.isBlank()) {
				sendBaseUrl = "https://fcm.googleapis.com";
			}
		}

		public static Fcm defaults() {
			return new Fcm("", "", "https://fcm.googleapis.com");
		}

		public boolean configured() {
			return projectId != null && !projectId.isBlank()
					&& accessToken != null && !accessToken.isBlank();
		}
	}
}
