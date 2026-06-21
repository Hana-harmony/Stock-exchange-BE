package com.hana.exchange.trade.domain;

import java.time.Instant;

public record TradeOrderPlacementResponse(
		String orderId,
		String accountId,
		String stockCode,
		String stockName,
		TradeSide side,
		long quantity,
		TradeOrderType orderType,
		String limitPriceUsd,
		String observedPriceUsd,
		TradeOrderStatus status,
		TradeExecutionResponse tradeExecution,
		String tradingMode,
		String message,
		Instant createdAt,
		Instant filledAt
) {
}
