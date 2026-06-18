package com.hana.exchange.market.domain;

import java.time.Instant;
import java.util.List;

public record MarketQuoteStreamPublishResponse(
		String stockCode,
		String market,
		int topicCount,
		List<String> topics,
		Instant publishedAt
) {
}
