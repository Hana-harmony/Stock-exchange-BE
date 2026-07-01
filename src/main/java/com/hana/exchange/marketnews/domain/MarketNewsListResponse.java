package com.hana.exchange.marketnews.domain;

import java.util.List;

import com.hana.exchange.marketnews.client.OmniLensMarketNewsListResponse;

public record MarketNewsListResponse(
		int newsCount,
		List<MarketNewsEventResponse> news
) {

	public MarketNewsListResponse {
		news = news == null ? List.of() : List.copyOf(news);
	}

	public static MarketNewsListResponse from(OmniLensMarketNewsListResponse response) {
		List<MarketNewsEventResponse> news = response.news().stream()
				.map(MarketNewsEventResponse::from)
				.toList();
		return new MarketNewsListResponse(response.newsCount(), news);
	}
}
