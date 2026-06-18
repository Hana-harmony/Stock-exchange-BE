package com.hana.exchange.trade.domain;

import java.time.Instant;

public record TradeExecutionResponse(
		String tradeId,
		String accountId,
		String stockCode,
		String stockName,
		TradeSide side,
		long quantity,
		String executionPriceUsd,
		String grossAmountUsd,
		String realizedPnlUsd,
		long remainingQuantity,
		String averagePriceUsdAfter,
		String cashBalanceUsdAfter,
		String tradingMode,
		Instant executedAt
) {
}
