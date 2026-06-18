package com.hana.exchange.account.domain;

import java.time.Instant;

public record TokenVerifyResponse(
		boolean valid,
		String userId,
		String username,
		String accountId,
		Instant issuedAt,
		Instant expiresAt
) {
}
