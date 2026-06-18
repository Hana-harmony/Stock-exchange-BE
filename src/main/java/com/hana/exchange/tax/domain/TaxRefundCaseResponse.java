package com.hana.exchange.tax.domain;

import java.time.Instant;
import java.util.List;

public record TaxRefundCaseResponse(
		String caseId,
		String accountId,
		String userId,
		int taxYear,
		String treatyCountry,
		String residenceCertificateFileName,
		String reducedTaxApplicationFileName,
		boolean advancePaymentRequested,
		TaxRefundCaseStatus status,
		String currency,
		String totalSellAmountUsd,
		String realizedProfitUsd,
		String realizedLossUsd,
		String netRealizedPnlUsd,
		String taxableRealizedPnlUsd,
		String estimatedWithholdingTaxUsd,
		String estimatedTreatyTaxUsd,
		String estimatedRefundUsd,
		boolean advancePaymentEligible,
		int matchedTradeCount,
		List<TaxMatchedTradeResponse> matchedTrades,
		String dataSource,
		Instant createdAt,
		Instant updatedAt
) {
}
