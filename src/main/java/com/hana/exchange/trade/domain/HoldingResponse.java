package com.hana.exchange.trade.domain;

import java.time.Instant;

public record HoldingResponse(
		String stockCode,
		String stockName,
		long quantity,
		String averagePriceUsd,
		String costBasisUsd,
		Instant updatedAt
) {
}
