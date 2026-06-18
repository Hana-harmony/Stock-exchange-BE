package com.hana.exchange.market.domain;

import java.time.Instant;
import java.util.List;

public record MarketOrderBookResponse(
		String dataSource,
		String stockCode,
		String market,
		String baseCurrency,
		String displayCurrency,
		List<Level> asks,
		List<Level> bids,
		Instant marketDataTime,
		Instant servedAt
) {
	public record Level(
			String priceKrw,
			String localCurrencyPrice,
			long quantity,
			long orderCount
	) {
	}
}
