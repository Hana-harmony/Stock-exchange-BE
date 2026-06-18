package com.hana.exchange.auth.domain;

import java.time.Instant;

public record AuthenticatedExchangeUser(
		String userId,
		String username,
		String accountId,
		Instant issuedAt,
		Instant expiresAt
) {
}
