package com.hana.exchange.market.domain;

import java.time.Instant;
import java.util.List;

public record MarketQuoteSnapshot(
		String dataSource,
		String marketCoverage,
		String userLanguage,
		String displayCurrency,
		String tradingMode,
		Transport transport,
		List<Quote> quotes,
		Instant servedAt
) {
	public record Transport(String snapshot, String realtime) {
	}

	public record Quote(
			String stockCode,
			String stockName,
			String currentPriceKrw,
			String localCurrency,
			String localCurrencyPrice,
			String fxRate,
			Instant fxRateTime,
			boolean fxStale
	) {
	}
}
