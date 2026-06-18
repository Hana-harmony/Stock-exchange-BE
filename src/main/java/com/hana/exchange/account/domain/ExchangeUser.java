package com.hana.exchange.account.domain;

import java.time.Instant;

public record ExchangeUser(
		String userId,
		String username,
		String passwordSalt,
		String passwordHash,
		Instant createdAt
) {
}
