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
		String marketFilter,
		Cache cache,
		int quoteCount,
		List<Quote> quotes,
		Instant servedAt
) {
	public record Transport(String snapshot, String realtime) {
	}

	public record Cache(
			String status,
			Instant cachedAt,
			Instant expiresAt,
			Instant staleUntil
	) {
	}

	public record Quote(
			String stockCode,
			String stockName,
			String market,
			String currentPriceKrw,
			String changeRate,
			long volume,
			String marketSession,
			String afterHoursPriceKrw,
			String afterHoursLocalCurrencyPrice,
			String afterHoursChangeRate,
			Long afterHoursVolume,
			Instant afterHoursMarketDataTime,
			String localCurrency,
			String localCurrencyPrice,
			String fxRate,
			Instant fxRateTime,
			String fxRateSource,
			boolean fxStale
	) {
	}
}
