package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

public record StockIntelligenceFeedItemResponse(
		String eventId,
		String sourceType,
		String title,
		String summary,
		String originalUrl,
		String primaryStockCode,
		List<String> relatedStocks,
		String sentiment,
		String importance,
		String riskLevel,
		boolean watchlistTarget,
		boolean holderTarget,
		Instant publishedAt,
		Instant receivedAt,
		int targetCount
) {
}
