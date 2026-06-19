package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

public record AlertEventMatchResult(
		AlertEvent event,
		List<AlertTargetResponse> targets,
		Instant matchedAt
) {
	public int targetCount() {
		return targets.size();
	}

	public AlertEventMatchResponse toResponse() {
		return new AlertEventMatchResponse(
				event.eventId(),
				event.idempotencyKey(),
				event.sourceType(),
				event.stockCode(),
				event.relatedStocks(),
				event.title(),
				event.summary(),
				event.originalUrl(),
				event.sentiment(),
				event.importance(),
				event.riskLevel(),
				event.glossaryTerms(),
				event.translationQualityFlags(),
				event.watchlistTarget(),
				event.holderTarget(),
				targetCount(),
				targets,
				matchedAt);
	}
}
