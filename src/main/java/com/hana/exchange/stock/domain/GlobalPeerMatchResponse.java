package com.hana.exchange.stock.domain;

import java.time.Instant;
import java.util.List;

public record GlobalPeerMatchResponse(
		String stockCode,
		String stockName,
		String headline,
		String summary,
		Peer primaryPeer,
		List<Peer> peers,
		String confidenceScore,
		String confidenceLevel,
		String modelVersion,
		String dataSource,
		Instant servedAt
) {
	public record Peer(
			int rank,
			String ticker,
			String companyName,
			String exchange,
			String country,
			String similarityScore,
			List<String> businessTags,
			String sector,
			String industry,
			String businessModel,
			String scaleBucket,
			Integer fiscalYear,
			String marketCapUsd,
			String revenueUsd,
			String operatingIncomeUsd,
			String netIncomeUsd,
			String financialDataSource,
			String financialSimilarityScore,
			List<String> matchedFactors,
			String rationale
	) {
	}
}
