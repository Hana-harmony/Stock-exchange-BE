package com.hana.exchange.trade.domain;

import java.time.Instant;
import java.util.List;

public record TradeLedgerHistoryResponse(
		String accountId,
		int tradeCount,
		List<TradeExecutionResponse> trades,
		Instant servedAt
) {
}
