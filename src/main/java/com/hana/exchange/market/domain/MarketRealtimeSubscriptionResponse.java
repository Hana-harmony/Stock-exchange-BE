package com.hana.exchange.market.domain;

import java.time.Instant;

public record MarketRealtimeSubscriptionResponse(
		String stockCode,
		String session,
		String status,
		String upstream,
		Instant processedAt
) {
}
