package com.hana.exchange.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record MockTradeLedgerEntry(
		String tradeId,
		String accountId,
		String userId,
		String stockCode,
		String stockName,
		TradeSide side,
		long quantity,
		BigDecimal executionPriceUsd,
		BigDecimal grossAmountUsd,
		BigDecimal realizedPnlUsd,
		long remainingQuantity,
		BigDecimal averagePriceUsdAfter,
		BigDecimal cashBalanceUsdAfter,
		Instant executedAt
) {
}
