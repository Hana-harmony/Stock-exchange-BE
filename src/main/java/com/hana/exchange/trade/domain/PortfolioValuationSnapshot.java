package com.hana.exchange.trade.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioValuationSnapshot(
		String snapshotId,
		String accountId,
		String userId,
		String currency,
		BigDecimal cashBalanceUsd,
		BigDecimal totalMarketValueUsd,
		BigDecimal totalAssetValueUsd,
		BigDecimal realizedPnlUsd,
		BigDecimal unrealizedPnlUsd,
		int holdingCount,
		Instant valuedAt
) {
}
