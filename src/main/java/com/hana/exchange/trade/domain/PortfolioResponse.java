package com.hana.exchange.trade.domain;

import java.time.Instant;
import java.util.List;

public record PortfolioResponse(
		String userId,
		String accountId,
		String currency,
		String cashBalanceUsd,
		String realizedPnlUsd,
		String tradingMode,
		List<HoldingResponse> holdings,
		List<TradeExecutionResponse> recentTrades,
		Instant servedAt
) {
}
