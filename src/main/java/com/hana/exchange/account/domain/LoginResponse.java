package com.hana.exchange.account.domain;

import java.time.Instant;

public record LoginResponse(
		String userId,
		String username,
		String accountId,
		String tokenType,
		String accessToken,
		Instant issuedAt,
		Instant expiresAt
) {
}
