package com.hana.exchange.trade.domain;

import java.time.Instant;
import java.util.List;

public record TradeOrderHistoryResponse(
		String accountId,
		int orderCount,
		List<TradeOrderPlacementResponse> orders,
		Instant servedAt
) {
}
