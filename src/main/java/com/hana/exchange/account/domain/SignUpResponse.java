package com.hana.exchange.account.domain;

import java.time.Instant;

public record SignUpResponse(
		String userId,
		String username,
		String accountId,
		String currency,
		String cashBalanceUsd,
		String tradingMode,
		Instant createdAt
) {
}
