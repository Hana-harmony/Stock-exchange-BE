package com.hana.exchange.alert.domain;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AlertEventIngestRequest(
		@NotBlank
		@JsonAlias("alertId")
		String eventId,

		@NotBlank
		@JsonAlias({"duplicateKey", "clusterKey"})
		String idempotencyKey,

		@NotBlank
		String sourceType,

		@NotBlank
		@JsonAlias("translatedTitle")
		String title,

		@NotBlank
		String summary,

		AlertSummaryLines summaryLines,

		@Size(max = 2000)
		String translatedSummary,

		@Size(max = 20000)
		String originalContent,

		@Size(max = 20000)
		String translatedContent,

		@Size(max = 20)
		List<@Size(max = 1000) String> imageUrls,

		@Size(max = 40)
		String contentAvailability,

		@NotBlank
		String originalUrl,

		@NotBlank
		@Pattern(regexp = "\\d{6}")
		String stockCode,

		@NotNull
		@Size(max = 20)
		List<@Pattern(regexp = "\\d{6}") String> relatedStocks,

		@Size(max = 50)
		List<@Valid AlertGlossaryTerm> glossaryTerms,

		@Size(max = 20)
		List<@Size(max = 80) String> translationQualityFlags,

		@Size(max = 128)
		String clusterKey,

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
	public AlertEventIngestRequest {
		summaryLines = summaryLines == null ? AlertSummaryLines.fromSummary(summary) : summaryLines;
		translatedSummary = translatedSummary == null ? "" : translatedSummary;
		originalContent = originalContent == null ? "" : originalContent;
		translatedContent = translatedContent == null ? "" : translatedContent;
		imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
		contentAvailability = contentAvailability == null || contentAvailability.isBlank()
				? "SUMMARY_ONLY"
				: contentAvailability;
		glossaryTerms = glossaryTerms == null ? List.of() : List.copyOf(glossaryTerms);
		translationQualityFlags = translationQualityFlags == null
				? List.of()
				: List.copyOf(translationQualityFlags);
		clusterKey = clusterKey == null || clusterKey.isBlank() ? idempotencyKey : clusterKey;
	}
}
