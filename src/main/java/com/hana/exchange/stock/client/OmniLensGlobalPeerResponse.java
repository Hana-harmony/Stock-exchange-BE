package com.hana.exchange.stock.client;

import java.math.BigDecimal;
import java.util.List;

public record OmniLensGlobalPeerResponse(
		String stockCode,
		String stockName,
		String stockNameEn,
		String headline,
		String summary,
		OmniLensGlobalPeerMatch primaryPeer,
		List<OmniLensGlobalPeerMatch> peers,
		BigDecimal confidenceScore,
		String confidenceLevel,
		String modelVersion,
		String source
) {
}
