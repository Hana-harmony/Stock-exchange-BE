package com.hana.exchange.marketnews.client;

import java.util.List;

public record OmniLensMarketNewsListResponse(
		int newsCount,
		List<OmniLensMarketNewsEvent> news
) {

	public OmniLensMarketNewsListResponse {
		news = news == null ? List.of() : List.copyOf(news);
	}
}
