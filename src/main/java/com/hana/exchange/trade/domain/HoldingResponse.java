package com.hana.exchange.trade.domain;

import java.time.Instant;

public record HoldingResponse(
		String stockCode,
		String stockName,
		long quantity,
		String averagePriceUsd,
		String costBasisUsd,
		String currentPriceUsd,
		String marketValueUsd,
		String unrealizedPnlUsd,
		String unrealizedPnlRate,
		Instant marketDataTime,
		Instant updatedAt
) {
}
