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
		List<AlertGlossaryTerm> glossaryTerms,
		List<String> translationQualityFlags,
		boolean watchlistTarget,
		boolean holderTarget,
		Instant publishedAt,
		Instant receivedAt,
		int targetCount
) {
}
