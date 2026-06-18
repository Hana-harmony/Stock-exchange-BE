package com.hana.exchange.account.application;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

	private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
	private static final int RANDOM_LENGTH = 12;

	private final SecureRandom secureRandom = new SecureRandom();

	public String newUserId() {
		return "USR-" + randomToken();
	}

	public String newAccountId() {
		return "ACC-" + randomToken();
	}

	public String newLedgerEntryId() {
		return "LED-" + randomToken();
	}

	private String randomToken() {
		StringBuilder token = new StringBuilder(RANDOM_LENGTH);
		for (int index = 0; index < RANDOM_LENGTH; index++) {
			token.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
		}
		return token.toString();
	}
}
