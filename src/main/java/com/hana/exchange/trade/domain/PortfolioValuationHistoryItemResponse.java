package com.hana.exchange.trade.domain;

import java.time.Instant;

public record PortfolioValuationHistoryItemResponse(
		String snapshotId,
		String currency,
		String cashBalanceUsd,
		String totalMarketValueUsd,
		String totalAssetValueUsd,
		String realizedPnlUsd,
		String unrealizedPnlUsd,
		int holdingCount,
		Instant valuedAt
) {
}
