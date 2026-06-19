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
		List<AlertGlossaryTerm> glossaryTerms,
		List<String> translationQualityFlags,
		String sentiment,
		String importance,
		String riskLevel,
		boolean watchlistTarget,
		boolean holderTarget,
		Instant publishedAt,
		Instant receivedAt
) {
	public AlertEvent {
		relatedStocks = relatedStocks == null ? List.of() : List.copyOf(relatedStocks);
		glossaryTerms = glossaryTerms == null ? List.of() : List.copyOf(glossaryTerms);
		translationQualityFlags = translationQualityFlags == null
				? List.of()
				: List.copyOf(translationQualityFlags);
	}

	public List<String> matchingStockCodes() {
		LinkedHashSet<String> stockCodes = new LinkedHashSet<>();
		stockCodes.add(stockCode);
		stockCodes.addAll(relatedStocks);
		return new ArrayList<>(stockCodes);
	}
}
