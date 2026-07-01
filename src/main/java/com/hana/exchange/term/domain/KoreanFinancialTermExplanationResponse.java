package com.hana.exchange.term.domain;

import java.time.Instant;
import java.util.List;

public record KoreanFinancialTermExplanationResponse(
		String term,
		String normalizedTerm,
		String englishTerm,
		String category,
		String definition,
		String explanation,
		String example,
		String confidenceScore,
		String confidenceLevel,
		String displayMode,
		String source,
		boolean cacheable,
		int cacheTtlSeconds,
		List<FinancialTermEvidenceResponse> evidence,
		List<String> qualityFlags,
		String modelVersion,
		Instant generatedAt,
		boolean cacheHit,
		long clickCount
) {
	public KoreanFinancialTermExplanationResponse {
		evidence = evidence == null ? List.of() : List.copyOf(evidence);
		qualityFlags = qualityFlags == null ? List.of() : List.copyOf(qualityFlags);
	}
}
