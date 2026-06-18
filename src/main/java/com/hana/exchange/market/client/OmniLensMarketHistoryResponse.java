package com.hana.exchange.market.client;

import java.util.List;

public record OmniLensMarketHistoryResponse(
		String stockCode,
		String interval,
		String baseCurrency,
		String localCurrency,
		List<OmniLensMarketHistoryPoint> points,
		String source
) {
}
