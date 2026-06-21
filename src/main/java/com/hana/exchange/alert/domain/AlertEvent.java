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
		AlertSummaryLines summaryLines,
		String translatedSummary,
		String originalContent,
		String translatedContent,
		List<String> imageUrls,
		String contentAvailability,
		String originalUrl,
		String stockCode,
		List<String> relatedStocks,
		List<AlertGlossaryTerm> glossaryTerms,
		List<String> translationQualityFlags,
		String clusterKey,
		String sentiment,
		String importance,
		String riskLevel,
		boolean watchlistTarget,
		boolean holderTarget,
		Instant publishedAt,
		Instant receivedAt
) {
	public AlertEvent {
		summaryLines = summaryLines == null ? AlertSummaryLines.fromSummary(summary) : summaryLines;
		translatedSummary = translatedSummary == null ? "" : translatedSummary;
		originalContent = originalContent == null ? "" : originalContent;
		translatedContent = translatedContent == null ? "" : translatedContent;
		imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
		contentAvailability = contentAvailability == null || contentAvailability.isBlank()
				? "SUMMARY_ONLY"
				: contentAvailability;
		relatedStocks = relatedStocks == null ? List.of() : List.copyOf(relatedStocks);
		glossaryTerms = glossaryTerms == null ? List.of() : List.copyOf(glossaryTerms);
		translationQualityFlags = translationQualityFlags == null
				? List.of()
				: List.copyOf(translationQualityFlags);
		clusterKey = clusterKey == null ? idempotencyKey : clusterKey;
	}

	public List<String> matchingStockCodes() {
		LinkedHashSet<String> stockCodes = new LinkedHashSet<>();
		stockCodes.add(stockCode);
		stockCodes.addAll(relatedStocks);
		return new ArrayList<>(stockCodes);
	}

	public AlertEvent(
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
			Instant receivedAt) {
		this(
				eventId,
				idempotencyKey,
				sourceType,
				title,
				summary,
				AlertSummaryLines.fromSummary(summary),
				"",
				"",
				"",
				List.of(),
				"SUMMARY_ONLY",
				originalUrl,
				stockCode,
				relatedStocks,
				glossaryTerms,
				translationQualityFlags,
				idempotencyKey,
				sentiment,
				importance,
				riskLevel,
				watchlistTarget,
				holderTarget,
				publishedAt,
				receivedAt);
	}
}
