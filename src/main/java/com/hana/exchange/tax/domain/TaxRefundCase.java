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
		String residenceCertificateDocumentId,
		String reducedTaxApplicationDocumentId,
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
	public TaxRefundCase updateStatus(TaxRefundCaseStatus status, Instant updatedAt) {
		return new TaxRefundCase(
				caseId,
				accountId,
				userId,
				taxYear,
				treatyCountry,
				residenceCertificateFileName,
				reducedTaxApplicationFileName,
				residenceCertificateDocumentId,
				reducedTaxApplicationDocumentId,
				advancePaymentRequested,
				status,
				totalSellAmountUsd,
				realizedProfitUsd,
				realizedLossUsd,
				netRealizedPnlUsd,
				taxableRealizedPnlUsd,
				estimatedWithholdingTaxUsd,
				estimatedTreatyTaxUsd,
				estimatedRefundUsd,
				advancePaymentEligible,
				matchedTradeIds,
				createdAt,
				updatedAt);
	}
}
