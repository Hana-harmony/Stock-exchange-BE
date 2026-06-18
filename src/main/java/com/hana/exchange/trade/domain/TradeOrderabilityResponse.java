package com.hana.exchange.trade.domain;

import java.time.Instant;
import java.util.List;

public record TradeOrderabilityResponse(
		String accountId,
		String stockCode,
		String market,
		TradeSide side,
		long quantity,
		boolean canPlaceMockOrder,
		List<String> blockingReasons,
		List<String> warnings,
		String orderabilitySource,
		String tradingMode,
		Instant checkedAt
) {
}
