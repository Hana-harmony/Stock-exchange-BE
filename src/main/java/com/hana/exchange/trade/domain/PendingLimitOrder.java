package com.hana.exchange.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PendingLimitOrder(
		String orderId,
		String accountId,
		String userId,
		String stockCode,
		String stockName,
		TradeSide side,
		long quantity,
		BigDecimal limitPriceUsd,
		BigDecimal observedPriceUsd,
		TradeOrderStatus status,
		String tradeId,
		Instant createdAt,
		Instant filledAt
) {
	public PendingLimitOrder filled(String tradeId, BigDecimal observedPriceUsd, Instant filledAt) {
		return new PendingLimitOrder(
				orderId,
				accountId,
				userId,
				stockCode,
				stockName,
				side,
				quantity,
				limitPriceUsd,
				observedPriceUsd,
				TradeOrderStatus.FILLED,
				tradeId,
				createdAt,
				filledAt);
	}
}
