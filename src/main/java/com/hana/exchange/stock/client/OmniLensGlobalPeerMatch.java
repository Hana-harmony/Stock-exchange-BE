package com.hana.exchange.stock.client;

import java.math.BigDecimal;
import java.util.List;

public record OmniLensGlobalPeerMatch(
		int rank,
		String ticker,
		String companyName,
		String exchange,
		String country,
		BigDecimal similarityScore,
		List<String> businessTags,
		String sector,
		String industry,
		String businessModel,
		String scaleBucket,
		List<String> matchedFactors,
		String rationale
) {
}
