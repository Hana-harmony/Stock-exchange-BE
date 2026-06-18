package com.hana.exchange.account.domain;

import java.time.Instant;

public record AccountBalanceResponse(
		String userId,
		String accountId,
		String currency,
		String cashBalanceUsd,
		String lastLedgerEntryId,
		Instant updatedAt
) {
}
