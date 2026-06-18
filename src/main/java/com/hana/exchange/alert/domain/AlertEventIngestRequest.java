package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AlertEventIngestRequest(
		@NotBlank
		String eventId,

		@NotBlank
		String idempotencyKey,

		@NotBlank
		String sourceType,

		@NotBlank
		String title,

		@NotBlank
		String summary,

		@NotBlank
		String originalUrl,

		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String stockCode,

		@NotNull
		@Size(max = 20)
		List<@Pattern(regexp = "\\d{6}") String> relatedStocks,

		@NotBlank
		String sentiment,

		@NotBlank
		String importance,

		@NotBlank
		String riskLevel,

		boolean watchlistTarget,

		boolean holderTarget,

		@NotNull
		Instant publishedAt
) {
}
