package com.hana.exchange.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MockUsdAccount(
		String accountId,
		String userId,
		String currency,
		BigDecimal cashBalanceUsd,
		Instant createdAt,
		Instant updatedAt
) {
	public MockUsdAccount deposit(BigDecimal amountUsd, Instant updatedAt) {
		return new MockUsdAccount(
				accountId,
				userId,
				currency,
				cashBalanceUsd.add(amountUsd),
				createdAt,
				updatedAt);
	}
}
