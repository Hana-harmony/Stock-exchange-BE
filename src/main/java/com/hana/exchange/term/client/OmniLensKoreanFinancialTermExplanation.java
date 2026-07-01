package com.hana.exchange.term.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OmniLensKoreanFinancialTermExplanation(
		String term,
		String normalizedTerm,
		String englishTerm,
		String category,
		String definition,
		String explanation,
		String example,
		BigDecimal confidenceScore,
		String confidenceLevel,
		String displayMode,
		String source,
		boolean cacheable,
		int cacheTtlSeconds,
		List<OmniLensFinancialTermEvidence> evidence,
		List<String> qualityFlags,
		String modelVersion,
		Instant generatedAt,
		boolean cacheHit,
		long clickCount
) {
	public OmniLensKoreanFinancialTermExplanation {
		evidence = evidence == null ? List.of() : List.copyOf(evidence);
		qualityFlags = qualityFlags == null ? List.of() : List.copyOf(qualityFlags);
	}
}
