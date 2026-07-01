package com.hana.exchange.marketnews.client;

public interface OmniLensMarketNewsClient {

	OmniLensMarketNewsListResponse getLatest(int limit);

	OmniLensMarketNewsEvent getByNewsId(String newsId);
}
