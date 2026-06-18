package com.hana.exchange.alert.domain;

public record AlertStreamProcessingResult(
		boolean accepted,
		String status,
		String reason
) {
	public static AlertStreamProcessingResult acceptedResult() {
		return new AlertStreamProcessingResult(true, "ACCEPTED", null);
	}

	public static AlertStreamProcessingResult rejected(String reason) {
		return new AlertStreamProcessingResult(false, "REJECTED", reason);
	}

	public static AlertStreamProcessingResult dropped(String reason) {
		return new AlertStreamProcessingResult(false, "DROPPED", reason);
	}
}
