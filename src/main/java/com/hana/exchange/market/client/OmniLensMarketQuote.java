package com.hana.exchange.market.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OmniLensMarketQuote(
		String stockCode,
		String stockName,
		String stockNameEn,
		String market,
		BigDecimal currentPriceKrw,
		BigDecimal changeRate,
		long volume,
		BigDecimal executionPriceKrw,
		String baseCurrency,
		BigDecimal localCurrencyPrice,
		String localCurrency,
		long foreignOwnedQuantity,
		BigDecimal foreignOwnershipRate,
		BigDecimal foreignLimitExhaustionRate,
		LocalDate foreignOwnershipBaseDate,
		Instant marketDataTime,
		String source
) {
}
