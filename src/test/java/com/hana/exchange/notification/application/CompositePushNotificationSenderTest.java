package com.hana.exchange.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hana.exchange.config.NotificationPushProperties;
import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

class CompositePushNotificationSenderTest {

	@Test
	void sendsWithFirstDeliveredEnabledProviderAfterSkippedProvider() {
		CompositePushNotificationSender sender = new CompositePushNotificationSender(
				properties(List.of("FCM_PUSH", "LOCAL_NOOP_PUSH")),
				List.of(skipProvider("FCM_PUSH"), new LocalNoopPushNotificationSender()));

		NotificationDeliveryResult result = sender.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(result.provider()).isEqualTo("LOCAL_NOOP_PUSH");
		assertThat(result.deliveredAt()).isNotNull();
	}

	@Test
	void skipsWhenEnabledProviderIsNotConfigured() {
		CompositePushNotificationSender sender = new CompositePushNotificationSender(
				properties(List.of("UNKNOWN_PUSH")),
				List.of(new LocalNoopPushNotificationSender()));

		NotificationDeliveryResult result = sender.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
		assertThat(result.provider()).isEqualTo("UNKNOWN_PUSH");
		assertThat(result.errorMessage()).isEqualTo("Push provider is not configured");
	}

	@Test
	void defaultsToLocalNoopProviderWhenNoProviderIsConfigured() {
		NotificationPushProperties properties = properties(List.of());
		CompositePushNotificationSender sender = new CompositePushNotificationSender(
				properties,
				List.of(new LocalNoopPushNotificationSender()));

		NotificationDeliveryResult result = sender.send(notification());

		assertThat(properties.enabledProviders()).containsExactly("LOCAL_NOOP_PUSH");
		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(result.provider()).isEqualTo("LOCAL_NOOP_PUSH");
	}

	private NotificationPushProperties properties(List<String> enabledProviders) {
		return new NotificationPushProperties(
				false,
				10,
				3,
				Duration.ofSeconds(30),
				enabledProviders,
				new NotificationPushProperties.TokenVault(""),
				NotificationPushProperties.Fcm.defaults());
	}

	private PushProviderClient skipProvider(String provider) {
		return new PushProviderClient() {
			@Override
			public String provider() {
				return provider;
			}

			@Override
			public NotificationDeliveryResult send(NotificationItem notification) {
				return NotificationDeliveryResult.skipped(provider, "not configured");
			}
		};
	}

	private NotificationItem notification() {
		return new NotificationItem(
				"NTF-PROVIDER01",
				"ACC-PROVIDER01",
				"USR-PROVIDER01",
				"EVT-PROVIDER01",
				"ALERT_EVENT",
				"EVT-PROVIDER01",
				"DISCLOSURE",
				"Samsung disclosure update",
				"Translated AI summary",
				"https://news.example.com/original",
				"005930",
				List.of("005930"),
				List.of("WATCHLIST"),
				List.of(),
				List.of(),
				NotificationDeliveryStatus.PENDING,
				null,
				0,
				null,
				null,
				false,
				Instant.parse("2026-06-18T06:00:00Z"),
				null);
	}
}
