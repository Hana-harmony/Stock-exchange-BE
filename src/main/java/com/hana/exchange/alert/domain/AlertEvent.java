package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record AlertEvent(
		String eventId,
		String idempotencyKey,
		String sourceType,
		String title,
		String summary,
		String originalUrl,
		String stockCode,
		List<String> relatedStocks,
		String sentiment,
		String importance,
		String riskLevel,
		boolean watchlistTarget,
		boolean holderTarget,
		Instant publishedAt,
		Instant receivedAt
) {
	public List<String> matchingStockCodes() {
		LinkedHashSet<String> stockCodes = new LinkedHashSet<>();
		stockCodes.add(stockCode);
		stockCodes.addAll(relatedStocks);
		return new ArrayList<>(stockCodes);
	}
}
