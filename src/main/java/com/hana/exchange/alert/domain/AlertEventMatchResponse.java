package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

public record AlertEventMatchResponse(
		String eventId,
		String idempotencyKey,
		String sourceType,
		String stockCode,
		List<String> relatedStocks,
		String title,
		String summary,
		String originalUrl,
		String sentiment,
		String importance,
		String riskLevel,
		List<AlertGlossaryTerm> glossaryTerms,
		List<String> translationQualityFlags,
		boolean watchlistTarget,
		boolean holderTarget,
		int targetCount,
		List<AlertTargetResponse> targets,
		Instant matchedAt
) {
}
