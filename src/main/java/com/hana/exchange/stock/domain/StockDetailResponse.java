package com.hana.exchange.stock.domain;

import java.time.Instant;
import java.time.LocalDate;

public record StockDetailResponse(
		String stockCode,
		String stockName,
		String market,
		String sector,
		String userLanguage,
		String baseCurrency,
		String displayCurrency,
		String currentPriceKrw,
		String localCurrencyPrice,
		String changeRate,
		long volume,
		Instant marketDataTime,
		long foreignOwnedQuantity,
		String foreignOwnershipRate,
		String foreignLimitExhaustionRate,
		LocalDate foreignOwnershipBaseDate,
		boolean viActive,
		String priceLimitState,
		boolean tradingHalted,
		boolean orderable,
		String dataSource,
		Instant servedAt
) {
}
