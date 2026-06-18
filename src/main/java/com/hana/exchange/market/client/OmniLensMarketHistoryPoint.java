package com.hana.exchange.market.client;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OmniLensMarketHistoryPoint(
		LocalDate tradeDate,
		BigDecimal openPriceKrw,
		BigDecimal highPriceKrw,
		BigDecimal lowPriceKrw,
		BigDecimal closePriceKrw,
		BigDecimal openLocalCurrencyPrice,
		BigDecimal highLocalCurrencyPrice,
		BigDecimal lowLocalCurrencyPrice,
		BigDecimal closeLocalCurrencyPrice,
		long volume,
		BigDecimal tradingValueKrw,
		boolean adjusted
) {
}
