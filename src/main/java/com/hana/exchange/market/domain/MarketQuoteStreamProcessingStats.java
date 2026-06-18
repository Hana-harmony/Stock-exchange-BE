package com.hana.exchange.market.domain;

import java.time.Instant;

public record MarketQuoteStreamProcessingStats(
		long acceptedCount,
		long publishedCount,
		long rejectedCount,
		long droppedCount,
		int bufferDepth,
		Instant lastMarketDataTime
) {
}
