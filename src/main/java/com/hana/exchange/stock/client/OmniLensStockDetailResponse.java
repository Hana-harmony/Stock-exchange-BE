package com.hana.exchange.stock.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OmniLensStockDetailResponse(
		String stockCode,
		String stockName,
		String stockNameEn,
		String market,
		String sector,
		BigDecimal currentPriceKrw,
		BigDecimal changeRate,
		long volume,
		String localCurrency,
		BigDecimal localCurrencyPrice,
		Instant marketDataTime,
		long foreignOwnedQuantity,
		BigDecimal foreignOwnershipRate,
		BigDecimal foreignLimitExhaustionRate,
		LocalDate foreignOwnershipBaseDate,
		boolean viActive,
		String priceLimitState,
		boolean tradingHalted,
		boolean orderable,
		String source
) {
}
