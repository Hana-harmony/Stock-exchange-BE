package com.hana.exchange.market.client;

public record OmniLensRealtimeSubscriptionResponse(
		String stockCode,
		String session,
		String status,
		String message
) {
}
