package com.hana.exchange.watchlist.domain;

import java.time.Instant;

public record WatchlistItemResponse(
		String stockCode,
		String stockName,
		String market,
		String targetingMode,
		Instant addedAt
) {
}
