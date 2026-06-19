package com.hana.exchange.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "exchange.notification.push")
public record NotificationPushProperties(
		boolean workerEnabled,
		int batchSize,
		int maxAttemptCount,
		Duration fixedDelay,
		List<String> enabledProviders,
		TokenVault tokenVault,
		Fcm fcm,
		Apns apns,
		WebPush webPush
) {
	public NotificationPushProperties(
			boolean workerEnabled,
			int batchSize,
			int maxAttemptCount,
			Duration fixedDelay,
			List<String> enabledProviders,
			TokenVault tokenVault,
			Fcm fcm) {
		this(
				workerEnabled,
				batchSize,
				maxAttemptCount,
				fixedDelay,
				enabledProviders,
				tokenVault,
				fcm,
				Apns.defaults(),
				WebPush.defaults());
	}

	@ConstructorBinding
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
		if (apns == null) {
			apns = Apns.defaults();
		}
		if (webPush == null) {
			webPush = WebPush.defaults();
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

	public record Apns(
			String bearerToken,
			String topic,
			String sendBaseUrl
	) {
		public Apns {
			if (sendBaseUrl == null || sendBaseUrl.isBlank()) {
				sendBaseUrl = "https://api.push.apple.com";
			}
		}

		public static Apns defaults() {
			return new Apns("", "", "https://api.push.apple.com");
		}

		public boolean configured() {
			return bearerToken != null && !bearerToken.isBlank()
					&& topic != null && !topic.isBlank();
		}
	}

	public record WebPush(
			String gatewayBaseUrl,
			String gatewayApiKey,
			String sendPath,
			String vapidSubject,
			String vapidPublicKey,
			String vapidPrivateKey
	) {
		public WebPush {
			if (sendPath == null || sendPath.isBlank()) {
				sendPath = "/send";
			}
			if (!sendPath.startsWith("/")) {
				sendPath = "/" + sendPath;
			}
		}

		public static WebPush defaults() {
			return new WebPush("", "", "/send", "", "", "");
		}

		public boolean configured() {
			return gatewayBaseUrl != null && !gatewayBaseUrl.isBlank()
					&& gatewayApiKey != null && !gatewayApiKey.isBlank()
					&& vapidSubject != null && !vapidSubject.isBlank()
					&& vapidPublicKey != null && !vapidPublicKey.isBlank()
					&& vapidPrivateKey != null && !vapidPrivateKey.isBlank();
		}
	}
}
