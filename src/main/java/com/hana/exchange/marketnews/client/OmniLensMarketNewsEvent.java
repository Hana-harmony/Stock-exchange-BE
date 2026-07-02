package com.hana.exchange.marketnews.client;

import java.time.Instant;
import java.util.List;

public record OmniLensMarketNewsEvent(
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

	public OmniLensMarketNewsEvent {
		imageUrls = imageUrls == null ? List.of() : List.copyOf(imageUrls);
	}
}
