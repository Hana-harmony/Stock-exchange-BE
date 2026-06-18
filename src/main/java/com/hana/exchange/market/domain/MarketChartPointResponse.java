package com.hana.exchange.market.domain;

import java.time.LocalDate;

public record MarketChartPointResponse(
		LocalDate tradeDate,
		String openPriceKrw,
		String highPriceKrw,
		String lowPriceKrw,
		String closePriceKrw,
		String localCurrency,
		String openLocalCurrencyPrice,
		String highLocalCurrencyPrice,
		String lowLocalCurrencyPrice,
		String closeLocalCurrencyPrice,
		long volume,
		String tradingValueKrw,
		boolean adjusted
) {
}
