package com.hana.exchange.account.domain;

import java.time.Instant;

public record LogoutResponse(
		boolean revoked,
		String sessionId,
		Instant revokedAt
) {
}
