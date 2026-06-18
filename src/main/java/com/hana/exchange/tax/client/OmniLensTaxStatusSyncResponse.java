package com.hana.exchange.tax.client;

import java.time.Instant;

public record OmniLensTaxStatusSyncResponse(
		String caseId,
		String status,
		Instant syncedAt,
		String source
) {
}
