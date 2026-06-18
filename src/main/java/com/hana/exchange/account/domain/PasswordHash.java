package com.hana.exchange.account.domain;

public record PasswordHash(
		String salt,
		String hash
) {
}
