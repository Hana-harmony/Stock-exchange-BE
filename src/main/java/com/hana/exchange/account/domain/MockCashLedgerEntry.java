package com.hana.exchange.account.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MockCashLedgerEntry(
		String ledgerEntryId,
		String accountId,
		String type,
		BigDecimal amountUsd,
		BigDecimal balanceAfterUsd,
		Instant createdAt
) {
}
