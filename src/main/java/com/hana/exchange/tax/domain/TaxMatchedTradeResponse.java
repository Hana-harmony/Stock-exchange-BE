package com.hana.exchange.tax.domain;

import java.time.Instant;

public record TaxMatchedTradeResponse(
		String tradeId,
		String stockCode,
		String stockName,
		long quantity,
		String grossAmountUsd,
		String realizedPnlUsd,
		Instant executedAt
) {
}
