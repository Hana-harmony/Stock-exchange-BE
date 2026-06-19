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

class WebPushProviderClientTest {

	@Test
	void sendsEncryptedSubscriptionToConfiguredWebPushGateway() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		NotificationPushProperties properties = properties();
		NotificationDeviceTokenCipher cipher = new NotificationDeviceTokenCipher(properties);
		InMemoryNotificationDeviceTokenRepository repository = new InMemoryNotificationDeviceTokenRepository();
		repository.save(deviceToken(cipher.encrypt("https://push.example.test/subscriptions/abc")));
		WebPushProviderClient client = new WebPushProviderClient(properties, repository, cipher, builder);

		server.expect(once(), requestTo("https://webpush.test/send"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer gateway-api-key"))
				.andExpect(jsonPath("$.subscriptionEndpoint").value("https://push.example.test/subscriptions/abc"))
				.andExpect(jsonPath("$.notification.title").value("Samsung disclosure update"))
				.andExpect(jsonPath("$.data.eventId").value("EVT-WEB-001"))
				.andExpect(jsonPath("$.vapid.subject").value("mailto:ops@hana.example"))
				.andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(result.provider()).isEqualTo("WEB_PUSH");
		assertThat(result.deliveredAt()).isNotNull();
		server.verify();
	}

	@Test
	void skipsWhenWebPushCredentialOrTokenVaultIsMissing() {
		WebPushProviderClient client = new WebPushProviderClient(
				new NotificationPushProperties(
						false,
						10,
						3,
						Duration.ofSeconds(30),
						List.of("WEB_PUSH"),
						new NotificationPushProperties.TokenVault(""),
						NotificationPushProperties.Fcm.defaults(),
						NotificationPushProperties.Apns.defaults(),
						NotificationPushProperties.WebPush.defaults()),
				new InMemoryNotificationDeviceTokenRepository(),
				NotificationDeviceTokenCipher.disabled(),
				RestClient.builder());

		var result = client.send(notification());

		assertThat(result.status()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
		assertThat(result.provider()).isEqualTo("WEB_PUSH");
		assertThat(result.errorMessage()).contains("Web push gateway or VAPID credentials");
	}

	private NotificationPushProperties properties() {
		return new NotificationPushProperties(
				false,
				10,
				3,
				Duration.ofSeconds(30),
				List.of("WEB_PUSH"),
				new NotificationPushProperties.TokenVault(NotificationDeviceTokenCipherTest.vaultKey()),
				NotificationPushProperties.Fcm.defaults(),
				NotificationPushProperties.Apns.defaults(),
				new NotificationPushProperties.WebPush(
						"https://webpush.test",
						"gateway-api-key",
						"send",
						"mailto:ops@hana.example",
						"vapid-public-key",
						"vapid-private-key"));
	}

	private NotificationDeviceToken deviceToken(String encryptedToken) {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		return new NotificationDeviceToken(
				"NTD-WEB000001",
				"ACC-WEB000001",
				"USR-WEB000001",
				NotificationDevicePlatform.WEB,
				"WEB_PUSH",
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				"https:...ns/abc",
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
				"NTF-WEB0000001",
				"ACC-WEB000001",
				"USR-WEB000001",
				"EVT-WEB-001",
				"ALERT_EVENT",
				"EVT-WEB-001",
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
