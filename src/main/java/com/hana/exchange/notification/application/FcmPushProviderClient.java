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
public class FcmPushProviderClient implements PushProviderClient {

	private final NotificationPushProperties properties;
	private final NotificationDeviceTokenRepository deviceTokenRepository;
	private final NotificationDeviceTokenCipher deviceTokenCipher;
	private final RestClient restClient;

	public FcmPushProviderClient(
			NotificationPushProperties properties,
			NotificationDeviceTokenRepository deviceTokenRepository,
			NotificationDeviceTokenCipher deviceTokenCipher,
			RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.deviceTokenRepository = deviceTokenRepository;
		this.deviceTokenCipher = deviceTokenCipher;
		this.restClient = restClientBuilder.baseUrl(properties.fcm().sendBaseUrl()).build();
	}

	@Override
	public String provider() {
		return "FCM_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		if (!properties.fcm().configured()) {
			return NotificationDeliveryResult.skipped(provider(), "FCM project ID or access token is not configured");
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
			return NotificationDeliveryResult.skipped(provider(), "No active FCM device token is registered");
		}

		NotificationDeliveryResult lastResult = null;
		for (NotificationDeviceToken deviceToken : deviceTokens) {
			Optional<String> decryptedToken = deviceTokenCipher.decrypt(deviceToken.encryptedToken());
			if (decryptedToken.isEmpty()) {
				lastResult = NotificationDeliveryResult.skipped(provider(), "FCM device token cannot be decrypted");
				continue;
			}
			lastResult = sendToFcm(notification, decryptedToken.get());
			if (lastResult.status() == NotificationDeliveryStatus.DELIVERED) {
				return lastResult;
			}
		}
		return lastResult == null
				? NotificationDeliveryResult.skipped(provider(), "No FCM device token handled notification")
				: lastResult;
	}

	private NotificationDeliveryResult sendToFcm(NotificationItem notification, String deviceToken) {
		try {
			restClient.post()
					.uri("/v1/projects/{projectId}/messages:send", properties.fcm().projectId())
					.header("Authorization", "Bearer " + properties.fcm().accessToken())
					.body(Map.of("message", message(notification, deviceToken)))
					.retrieve()
					.toBodilessEntity();
			return NotificationDeliveryResult.delivered(provider(), Instant.now());
		}
		catch (RestClientException exception) {
			return NotificationDeliveryResult.failed(provider(), sanitize(exception.getMessage()));
		}
	}

	private Map<String, Object> message(NotificationItem notification, String deviceToken) {
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
		return Map.of(
				"token", deviceToken,
				"notification", Map.of(
						"title", notification.title(),
						"body", notification.summary()),
				"data", data);
	}

	private String sanitize(String message) {
		if (message == null || message.isBlank()) {
			return "FCM push delivery failed";
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
