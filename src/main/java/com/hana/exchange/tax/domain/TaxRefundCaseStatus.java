package com.hana.exchange.tax.domain;

public enum TaxRefundCaseStatus {
	NOT_SUBMITTED,
	READY_FOR_HANA_SYNC,
	NO_REFUNDABLE_PROFIT,
	SYNCED_WITH_HANA,
	REFUND_APPROVED,
	ADVANCE_PAID,
	RECAPTURE_RISK
}
