package com.hana.exchange.watchlist.domain;

import java.time.Instant;

public record WatchlistItem(
		String accountId,
		String userId,
		String stockCode,
		String stockName,
		String market,
		String targetingMode,
		Instant addedAt
) {
}
