package com.hana.exchange.tax.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TaxRefundCase(
		String caseId,
		String accountId,
		String userId,
		int taxYear,
		String treatyCountry,
		String residenceCertificateFileName,
		String reducedTaxApplicationFileName,
		boolean advancePaymentRequested,
		TaxRefundCaseStatus status,
		BigDecimal totalSellAmountUsd,
		BigDecimal realizedProfitUsd,
		BigDecimal realizedLossUsd,
		BigDecimal netRealizedPnlUsd,
		BigDecimal taxableRealizedPnlUsd,
		BigDecimal estimatedWithholdingTaxUsd,
		BigDecimal estimatedTreatyTaxUsd,
		BigDecimal estimatedRefundUsd,
		boolean advancePaymentEligible,
		List<String> matchedTradeIds,
		Instant createdAt,
		Instant updatedAt
) {
}
