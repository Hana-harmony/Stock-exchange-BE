package com.hana.exchange.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hana.exchange.config.NotificationPushProperties;

class NotificationDeviceTokenCipherTest {

	@Test
	void encryptsAndDecryptsDeviceTokenWithConfiguredVaultKey() {
		NotificationDeviceTokenCipher cipher = new NotificationDeviceTokenCipher(properties(vaultKey()));

		String encryptedToken = cipher.encrypt("fcm-device-token-0123456789");

		assertThat(encryptedToken).startsWith("v1:");
		assertThat(encryptedToken).doesNotContain("fcm-device-token");
		assertThat(cipher.decrypt(encryptedToken)).contains("fcm-device-token-0123456789");
	}

	@Test
	void returnsEmptyWhenVaultIsNotConfigured() {
		NotificationDeviceTokenCipher cipher = NotificationDeviceTokenCipher.disabled();

		assertThat(cipher.encrypt("fcm-device-token-0123456789")).isNull();
		assertThat(cipher.decrypt("v1:bad:bad")).isEmpty();
	}

	static String vaultKey() {
		return Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
	}

	private NotificationPushProperties properties(String vaultKey) {
		return new NotificationPushProperties(
				false,
				10,
				3,
				Duration.ofSeconds(30),
				List.of("FCM_PUSH"),
				new NotificationPushProperties.TokenVault(vaultKey),
				NotificationPushProperties.Fcm.defaults());
	}
}
