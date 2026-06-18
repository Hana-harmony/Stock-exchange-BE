package com.hana.exchange.tax.client;

import java.time.Instant;
import java.util.List;

public record OmniLensTaxStatusSyncRequest(
		String caseId,
		String accountId,
		String userId,
		int taxYear,
		String treatyCountry,
		String estimatedRefundUsd,
		boolean advancePaymentRequested,
		boolean advancePaymentEligible,
		List<String> matchedTradeIds,
		Instant requestedAt
) {
}
