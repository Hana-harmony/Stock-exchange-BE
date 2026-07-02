package com.hana.exchange.marketnews.domain;

import java.time.Instant;
import java.util.List;

import com.hana.exchange.marketnews.client.OmniLensMarketNewsEvent;

public record MarketNewsEventResponse(
		String newsId,
		String query,
		String title,
		String summary,
		String originalContent,
		List<String> imageUrls,
		String contentAvailability,
		String originalUrl,
		String canonicalUrl,
		String sourceLicensePolicy,
		String duplicateKey,
		Instant publishedAt,
		Instant createdAt
) {

	public MarketNewsEventResponse {
		imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
	}

	public static MarketNewsEventResponse from(OmniLensMarketNewsEvent event) {
		return new MarketNewsEventResponse(
				event.newsId(),
				event.query(),
				event.title(),
				event.summary(),
				event.originalContent(),
				event.imageUrls(),
				event.contentAvailability(),
				event.originalUrl(),
				event.canonicalUrl(),
				event.sourceLicensePolicy(),
				event.duplicateKey(),
				event.publishedAt(),
				event.createdAt());
	}
}
