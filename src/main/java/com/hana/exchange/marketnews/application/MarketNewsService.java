package com.hana.exchange.marketnews.application;

import org.springframework.stereotype.Service;

import com.hana.exchange.marketnews.client.OmniLensMarketNewsClient;
import com.hana.exchange.marketnews.domain.MarketNewsEventResponse;
import com.hana.exchange.marketnews.domain.MarketNewsListResponse;

@Service
public class MarketNewsService {

	private final OmniLensMarketNewsClient marketNewsClient;

	public MarketNewsService(OmniLensMarketNewsClient marketNewsClient) {
		this.marketNewsClient = marketNewsClient;
	}

	public MarketNewsListResponse getLatest(int limit) {
		return MarketNewsListResponse.from(marketNewsClient.getLatest(limit));
	}

	public MarketNewsEventResponse getByNewsId(String newsId) {
		return MarketNewsEventResponse.from(marketNewsClient.getByNewsId(newsId));
	}
}
