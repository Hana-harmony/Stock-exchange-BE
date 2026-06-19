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
public class WebPushProviderClient implements PushProviderClient {

	private final NotificationPushProperties properties;
	private final NotificationDeviceTokenRepository deviceTokenRepository;
	private final NotificationDeviceTokenCipher deviceTokenCipher;
	private final RestClient restClient;

	public WebPushProviderClient(
			NotificationPushProperties properties,
			NotificationDeviceTokenRepository deviceTokenRepository,
			NotificationDeviceTokenCipher deviceTokenCipher,
			RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.deviceTokenRepository = deviceTokenRepository;
		this.deviceTokenCipher = deviceTokenCipher;
		this.restClient = properties.webPush().gatewayBaseUrl() == null
				|| properties.webPush().gatewayBaseUrl().isBlank()
						? restClientBuilder.build()
						: restClientBuilder.baseUrl(properties.webPush().gatewayBaseUrl()).build();
	}

	@Override
	public String provider() {
		return "WEB_PUSH";
	}

	@Override
	public NotificationDeliveryResult send(NotificationItem notification) {
		if (!properties.webPush().configured()) {
			return NotificationDeliveryResult.skipped(provider(), "Web push gateway or VAPID credentials are not configured");
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
			return NotificationDeliveryResult.skipped(provider(), "No active web push subscription is registered");
		}

		NotificationDeliveryResult lastResult = null;
		for (NotificationDeviceToken deviceToken : deviceTokens) {
			Optional<String> decryptedToken = deviceTokenCipher.decrypt(deviceToken.encryptedToken());
			if (decryptedToken.isEmpty()) {
				lastResult = NotificationDeliveryResult.skipped(provider(), "Web push subscription cannot be decrypted");
				continue;
			}
			lastResult = sendToGateway(notification, decryptedToken.get());
			if (lastResult.status() == NotificationDeliveryStatus.DELIVERED) {
				return lastResult;
			}
		}
		return lastResult == null
				? NotificationDeliveryResult.skipped(provider(), "No web push subscription handled notification")
				: lastResult;
	}

	private NotificationDeliveryResult sendToGateway(NotificationItem notification, String subscriptionEndpoint) {
		try {
			restClient.post()
					.uri(properties.webPush().sendPath())
					.header("Authorization", "Bearer " + properties.webPush().gatewayApiKey())
					.body(payload(notification, subscriptionEndpoint))
					.retrieve()
					.toBodilessEntity();
			return NotificationDeliveryResult.delivered(provider(), Instant.now());
		}
		catch (RestClientException exception) {
			return NotificationDeliveryResult.failed(provider(), sanitize(exception.getMessage()));
		}
	}

	private Map<String, Object> payload(NotificationItem notification, String subscriptionEndpoint) {
		return Map.of(
				"subscriptionEndpoint", subscriptionEndpoint,
				"notification", Map.of(
						"title", notification.title(),
						"body", notification.summary()),
				"data", data(notification),
				"vapid", Map.of(
						"subject", properties.webPush().vapidSubject(),
						"publicKey", properties.webPush().vapidPublicKey(),
						"privateKey", properties.webPush().vapidPrivateKey()));
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
			return "Web push delivery failed";
		}
		return message.length() > 500 ? message.substring(0, 500) : message;
	}
}
