package com.hana.exchange.account.application;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Component;

import com.hana.exchange.account.domain.PasswordHash;

@Component
public class PasswordHasher {

	private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	private static final int SALT_BYTES = 16;
	private static final int ITERATIONS = 120_000;
	private static final int KEY_LENGTH = 256;

	private final SecureRandom secureRandom = new SecureRandom();

	public PasswordHash hash(String password) {
		byte[] salt = new byte[SALT_BYTES];
		secureRandom.nextBytes(salt);
		byte[] hash = pbkdf2(password.toCharArray(), salt);
		return new PasswordHash(
				Base64.getEncoder().encodeToString(salt),
				Base64.getEncoder().encodeToString(hash));
	}

	private byte[] pbkdf2(char[] password, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
			SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
			return factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
			throw new IllegalStateException("Password hashing failed", exception);
		}
	}
}
