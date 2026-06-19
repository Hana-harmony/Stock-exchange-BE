package com.hana.exchange.notification.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.hana.exchange.config.NotificationPushProperties;
import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationDeviceToken;
import com.hana.exchange.notification.domain.NotificationItem;

@Component
public class ApnsPushProviderClient implements PushProviderClient {

	private final NotificationPushProperties properties;
	private final NotificationDeviceTokenRepository deviceTokenRepository;
	private final NotificationDeviceTokenCipher deviceTokenCipher;
	private final RestClient restClient;

	public ApnsPushProviderClient(
			NotificationPushProperties properties,
			NotificationDeviceTokenRepository deviceTokenRepository,
			NotificationDeviceTokenCipher deviceTokenCipher,
			RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.deviceTokenRepository = deviceTokenRepository;
		this.deviceTokenCipher = deviceTokenCipher;
		this.restClient = restClientBuilder.baseUrl(properties.apns().sendBaseUrl()).build();
	}

	@Override
	public String provider() {
		return "APNS_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		if (!properties.apns().configured()) {
			return NotificationDeliveryResult.skipped(provider(), "APNS bearer token or topic is not configured");
		}
		if (!deviceTokenCipher.configured()) {
			return NotificationDeliveryResult.skipped(provider(), "Notification token vault is not configured");
		}
		List<NotificationDeviceToken> deviceTokens = deviceTokenRepository.findByAccountId(notification.accountId())
				.stream()
				.filter(NotificationDeviceToken::active)
				.filter(deviceToken -> provider().equals(deviceToken.provider()))
				.toList();
		if (deviceTokens.isEmpty()) {
			return NotificationDeliveryResult.skipped(provider(), "No active APNS device token is registered");
		}

		NotificationDeliveryResult lastResult = null;
		for (NotificationDeviceToken deviceToken : deviceTokens) {
			Optional<String> decryptedToken = deviceTokenCipher.decrypt(deviceToken.encryptedToken());
			if (decryptedToken.isEmpty()) {
				lastResult = NotificationDeliveryResult.skipped(provider(), "APNS device token cannot be decrypted");
				continue;
			}
			lastResult = sendToApns(notification, decryptedToken.get());
			if (lastResult.status() == NotificationDeliveryStatus.DELIVERED) {
				return lastResult;
			}
		}
		return lastResult == null
				? NotificationDeliveryResult.skipped(provider(), "No APNS device token handled notification")
				: lastResult;
	}

	private NotificationDeliveryResult sendToApns(NotificationItem notification, String deviceToken) {
		try {
			restClient.post()
					.uri("/3/device/{deviceToken}", deviceToken)
					.header("Authorization", "Bearer " + properties.apns().bearerToken())
					.header("apns-topic", properties.apns().topic())
					.header("apns-push-type", "alert")
					.header("apns-priority", "10")
					.body(payload(notification))
					.retrieve()
					.toBodilessEntity();
			return NotificationDeliveryResult.delivered(provider(), Instant.now());
		}
		catch (RestClientException exception) {
			return NotificationDeliveryResult.failed(provider(), sanitize(exception.getMessage()));
		}
	}

	private Map<String, Object> payload(NotificationItem notification) {
		return Map.of(
				"aps", Map.of(
						"alert", Map.of(
								"title", notification.title(),
								"body", notification.summary()),
						"sound", "default"),
				"data", data(notification));
	}

	private Map<String, String> data(NotificationItem notification) {
		Map<String, String> data = new LinkedHashMap<>();
		data.put("notificationId", notification.notificationId());
		data.put("subjectType", notification.subjectType());
		data.put("subjectId", notification.subjectId());
		data.put("sourceType", notification.sourceType());
		if (notification.eventId() != null) {
			data.put("eventId", notification.eventId());
		}
		if (notification.primaryStockCode() != null) {
			data.put("stockCode", notification.primaryStockCode());
		}
		if (notification.originalUrl() != null && !notification.originalUrl().isBlank()) {
			data.put("originalUrl", notification.originalUrl());
		}
		return data;
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "APNS push delivery failed";
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
