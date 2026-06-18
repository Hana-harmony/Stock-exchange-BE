package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

public record StockIntelligenceFeedResponse(
		String stockCode,
		String dataSource,
		int itemCount,
		List<StockIntelligenceFeedItemResponse> items,
		Instant servedAt
) {
}
