package com.hana.exchange.market.client;

import java.time.Instant;
import java.util.List;

public record OmniLensOrderBookResponse(
		String stockCode,
		String market,
		String baseCurrency,
		String localCurrency,
		List<OmniLensOrderBookLevel> asks,
		List<OmniLensOrderBookLevel> bids,
		Instant marketDataTime,
		String source
) {
}
