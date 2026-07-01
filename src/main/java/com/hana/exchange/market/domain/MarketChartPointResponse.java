package com.hana.exchange.market.domain;

public record MarketChartPointResponse(
		String tradeDate,
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
