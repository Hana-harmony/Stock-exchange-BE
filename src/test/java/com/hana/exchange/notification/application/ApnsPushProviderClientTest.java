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

class ApnsPushProviderClientTest {

	@Test
	void sendsEncryptedDeviceTokenToApnsEndpoint() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		NotificationPushProperties properties = properties();
		NotificationDeviceTokenCipher cipher = new NotificationDeviceTokenCipher(properties);
		InMemoryNotificationDeviceTokenRepository repository = new InMemoryNotificationDeviceTokenRepository();
		repository.save(deviceToken(cipher.encrypt("apns-device-token-0123456789")));
		ApnsPushProviderClient client = new ApnsPushProviderClient(properties, repository, cipher, builder);

		server.expect(once(), requestTo("https://apns.test/3/device/apns-device-token-0123456789"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-apns-token"))
				.andExpect(header("apns-topic", "com.hana.exchange"))
				.andExpect(header("apns-push-type", "alert"))
				.andExpect(jsonPath("$.aps.alert.title").value("Samsung disclosure update"))
				.andExpect(jsonPath("$.data.eventId").value("EVT-APNS-001"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(result.provider()).isEqualTo("APNS_PUSH");
		assertThat(result.deliveredAt()).isNotNull();
		server.verify();
	}

	@Test
	void skipsWhenApnsCredentialOrTokenVaultIsMissing() {
		ApnsPushProviderClient client = new ApnsPushProviderClient(
				new NotificationPushProperties(
						false,
						10,
						3,
						Duration.ofSeconds(30),
						List.of("APNS_PUSH"),
						new NotificationPushProperties.TokenVault(""),
						NotificationPushProperties.Fcm.defaults(),
						NotificationPushProperties.Apns.defaults(),
						NotificationPushProperties.WebPush.defaults()),
				new InMemoryNotificationDeviceTokenRepository(),
				NotificationDeviceTokenCipher.disabled(),
				RestClient.builder());

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
		assertThat(result.provider()).isEqualTo("APNS_PUSH");
		assertThat(result.errorMessage()).contains("APNS bearer token or topic");
	}

	private NotificationPushProperties properties() {
		return new NotificationPushProperties(
				false,
				10,
				3,
				Duration.ofSeconds(30),
				List.of("APNS_PUSH"),
				new NotificationPushProperties.TokenVault(NotificationDeviceTokenCipherTest.vaultKey()),
				NotificationPushProperties.Fcm.defaults(),
				new NotificationPushProperties.Apns("test-apns-token", "com.hana.exchange", "https://apns.test"),
				NotificationPushProperties.WebPush.defaults());
	}

	private NotificationDeviceToken deviceToken(String encryptedToken) {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		return new NotificationDeviceToken(
				"NTD-APNS00001",
				"ACC-APNS00001",
				"USR-APNS00001",
				NotificationDevicePlatform.IOS,
				"APNS_PUSH",
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				"apns-d...345678",
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
				"NTF-APNS000001",
				"ACC-APNS00001",
				"USR-APNS00001",
				"EVT-APNS-001",
				"ALERT_EVENT",
				"EVT-APNS-001",
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
