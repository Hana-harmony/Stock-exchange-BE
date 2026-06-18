package com.hana.exchange.stock.domain;

import java.time.Instant;
import java.util.List;

public record StockSearchResponse(
		String query,
		String marketFilter,
		String userLanguage,
		String displayCurrency,
		int resultCount,
		List<StockSearchItemResponse> results,
		Instant servedAt
) {
}
