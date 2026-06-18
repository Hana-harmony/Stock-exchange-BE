package com.hana.exchange.account.domain;

import java.time.Instant;

public record RefreshTokenResponse(
		String userId,
		String username,
		String accountId,
		String tokenType,
		String accessToken,
		String refreshToken,
		String sessionId,
		Instant issuedAt,
		Instant expiresAt,
		Instant refreshTokenExpiresAt
) {
}
