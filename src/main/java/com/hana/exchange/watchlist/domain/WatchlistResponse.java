package com.hana.exchange.watchlist.domain;

import java.time.Instant;
import java.util.List;

public record WatchlistResponse(
		String userId,
		String accountId,
		int itemCount,
		String targetingMode,
		List<WatchlistItemResponse> items,
		Instant servedAt
) {
}
