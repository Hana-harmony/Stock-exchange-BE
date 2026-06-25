package com.hana.exchange.market.client;

import java.math.BigDecimal;
import java.time.Instant;

public record OmniLensMarketIndex(
		String indexCode,
		String indexName,
		String market,
		BigDecimal currentValue,
		String changeSign,
		BigDecimal changeValue,
		BigDecimal changeRate,
		long accumulatedVolume,
		long accumulatedTradingValue,
		BigDecimal openValue,
		BigDecimal highValue,
		BigDecimal lowValue,
		Instant marketDataTime,
		String source
) {
}
