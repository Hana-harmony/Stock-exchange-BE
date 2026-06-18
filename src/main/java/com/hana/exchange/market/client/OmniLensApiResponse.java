package com.hana.exchange.market.client;

import java.time.Instant;

public record OmniLensApiResponse<T>(
		boolean success,
		int status,
		String code,
		String message,
		T data,
		Instant timestamp
) {
}
