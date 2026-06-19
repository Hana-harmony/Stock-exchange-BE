package com.hana.exchange.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.hana.exchange.config.NotificationPushProperties;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationDevicePlatform;
import com.hana.exchange.notification.domain.NotificationDeviceToken;
import com.hana.exchange.notification.domain.NotificationItem;

class FcmPushProviderClientTest {

	@Test
	void sendsEncryptedDeviceTokenToFcmHttpV1Endpoint() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		NotificationDeviceTokenCipher cipher = new NotificationDeviceTokenCipher(properties());
		InMemoryNotificationDeviceTokenRepository repository = new InMemoryNotificationDeviceTokenRepository();
		repository.save(deviceToken(cipher.encrypt("fcm-device-token-0123456789")));
		FcmPushProviderClient client = new FcmPushProviderClient(
				properties(),
				repository,
				cipher,
				builder);

		server.expect(once(), requestTo("https://fcm.test/v1/projects/hana-local/messages:send"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-access-token"))
				.andExpect(jsonPath("$.message.token").value("fcm-device-token-0123456789"))
				.andExpect(jsonPath("$.message.notification.title").value("Samsung disclosure update"))
				.andExpect(jsonPath("$.message.data.eventId").value("EVT-FCM-001"))
				.andRespond(withSuccess("{\"name\":\"projects/hana-local/messages/1\"}", MediaType.APPLICATION_JSON));

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(result.provider()).isEqualTo("FCM_PUSH");
		assertThat(result.deliveredAt()).isNotNull();
		server.verify();
	}

	@Test
	void skipsWhenFcmCredentialOrTokenVaultIsMissing() {
		FcmPushProviderClient client = new FcmPushProviderClient(
				new NotificationPushProperties(
						false,
						10,
						3,
						Duration.ofSeconds(30),
						List.of("FCM_PUSH"),
						new NotificationPushProperties.TokenVault(""),
						NotificationPushProperties.Fcm.defaults()),
				new InMemoryNotificationDeviceTokenRepository(),
				NotificationDeviceTokenCipher.disabled(),
				RestClient.builder());

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
		assertThat(result.provider()).isEqualTo("FCM_PUSH");
		assertThat(result.errorMessage()).contains("FCM project ID or access token");
	}

	private NotificationPushProperties properties() {
		return new NotificationPushProperties(
				false,
				10,
				3,
				Duration.ofSeconds(30),
				List.of("FCM_PUSH"),
				new NotificationPushProperties.TokenVault(NotificationDeviceTokenCipherTest.vaultKey()),
				new NotificationPushProperties.Fcm("hana-local", "test-access-token", "https://fcm.test"));
	}

	private NotificationDeviceToken deviceToken(String encryptedToken) {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		return new NotificationDeviceToken(
				"NTD-FCM0000001",
				"ACC-FCM0000001",
				"USR-FCM0000001",
				NotificationDevicePlatform.ANDROID,
				"FCM_PUSH",
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				"fcm-de...345678",
				encryptedToken,
				"1.0.0",
				"en_US",
				true,
				now,
				now,
				null);
	}

	private NotificationItem notification() {
		return new NotificationItem(
				"NTF-FCM0000001",
				"ACC-FCM0000001",
				"USR-FCM0000001",
				"EVT-FCM-001",
				"ALERT_EVENT",
				"EVT-FCM-001",
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
