package com.hana.exchange.trade.domain;

import java.time.Instant;
import java.util.List;

public record PortfolioValuationHistoryResponse(
		String accountId,
		String currency,
		int snapshotCount,
		List<PortfolioValuationHistoryItemResponse> snapshots,
		Instant servedAt
) {
}
