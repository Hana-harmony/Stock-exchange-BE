package com.hana.exchange.notification.application;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hana.exchange.config.NotificationPushProperties;

@Component
public class NotificationDeviceTokenCipher {

	private static final String PREFIX = "v1";
	private static final int GCM_TAG_BITS = 128;
	private static final int IV_BYTES = 12;

	private final SecretKeySpec keySpec;
	private final SecureRandom secureRandom;

	@Autowired
	public NotificationDeviceTokenCipher(NotificationPushProperties properties) {
		this(keySpec(properties.tokenVault().encryptionKey()), new SecureRandom());
	}

	private NotificationDeviceTokenCipher(SecretKeySpec keySpec, SecureRandom secureRandom) {
		this.keySpec = keySpec;
		this.secureRandom = secureRandom;
	}

	public static NotificationDeviceTokenCipher disabled() {
		return new NotificationDeviceTokenCipher(null, new SecureRandom());
	}

	public boolean configured() {
		return keySpec != null;
	}

	public String encrypt(String plainToken) {
		if (!configured() || plainToken == null || plainToken.isBlank()) {
			return null;
		}
		byte[] iv = new byte[IV_BYTES];
		secureRandom.nextBytes(iv);
		try {
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));
			return PREFIX + ":"
					+ Base64.getEncoder().encodeToString(iv) + ":"
					+ Base64.getEncoder().encodeToString(encrypted);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Notification device token encryption failed", exception);
		}
	}

	public Optional<String> decrypt(String encryptedToken) {
		if (!configured() || encryptedToken == null || encryptedToken.isBlank()) {
			return Optional.empty();
		}
		String[] parts = encryptedToken.split(":", 3);
		if (parts.length != 3 || !PREFIX.equals(parts[0])) {
			return Optional.empty();
		}
		try {
			byte[] iv = Base64.getDecoder().decode(parts[1]);
			byte[] encrypted = Base64.getDecoder().decode(parts[2]);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
			return Optional.of(new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
		}
		catch (IllegalArgumentException | GeneralSecurityException exception) {
			return Optional.empty();
		}
	}

	private static SecretKeySpec keySpec(String encodedKey) {
		if (encodedKey == null || encodedKey.isBlank()) {
			return null;
		}
		byte[] key = Base64.getDecoder().decode(encodedKey);
		if (key.length != 16 && key.length != 24 && key.length != 32) {
			throw new IllegalArgumentException("Notification token vault key must be 128, 192, or 256 bit Base64");
		}
		return new SecretKeySpec(key, "AES");
	}
}
