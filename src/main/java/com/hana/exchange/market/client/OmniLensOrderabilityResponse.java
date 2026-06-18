package com.hana.exchange.market.client;

import java.time.Instant;

public record OmniLensOrderabilityResponse(
		String stockCode,
		String market,
		boolean orderable,
		String orderBlockedReason,
		boolean foreignLimitExceeded,
		boolean viActive,
		String priceLimitState,
		boolean tradingHalted,
		Instant checkedAt,
		String source
) {
}
