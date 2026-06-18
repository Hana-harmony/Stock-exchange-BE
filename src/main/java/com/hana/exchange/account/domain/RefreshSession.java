package com.hana.exchange.account.domain;

import java.time.Instant;

public record RefreshSession(
		String sessionId,
		String userId,
		String accountId,
		String refreshTokenHash,
		Instant issuedAt,
		Instant expiresAt,
		Instant revokedAt,
		String replacedBySessionId
) {

	public boolean activeAt(Instant now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public RefreshSession revoke(Instant revokedAt, String replacedBySessionId) {
		return new RefreshSession(
				sessionId,
				userId,
				accountId,
				refreshTokenHash,
				issuedAt,
				expiresAt,
				revokedAt,
				replacedBySessionId);
	}
}
