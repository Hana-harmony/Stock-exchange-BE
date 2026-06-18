package com.hana.exchange.market.domain;

public record MarketQuoteStreamProcessingResult(
		boolean accepted,
		String status,
		String reason
) {
	public static MarketQuoteStreamProcessingResult acceptedResult() {
		return new MarketQuoteStreamProcessingResult(true, "ACCEPTED", null);
	}

	public static MarketQuoteStreamProcessingResult rejected(String reason) {
		return new MarketQuoteStreamProcessingResult(false, "REJECTED", reason);
	}

	public static MarketQuoteStreamProcessingResult dropped(String reason) {
		return new MarketQuoteStreamProcessingResult(false, "DROPPED", reason);
	}
}
