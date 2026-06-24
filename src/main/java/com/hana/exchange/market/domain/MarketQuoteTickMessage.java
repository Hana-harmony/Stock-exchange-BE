package com.hana.exchange.market.domain;

import java.time.Instant;

public record MarketQuoteTickMessage(
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
		boolean fxStale,
		Instant marketDataTime,
		String source,
		String transport,
		Instant publishedAt
) {
}
