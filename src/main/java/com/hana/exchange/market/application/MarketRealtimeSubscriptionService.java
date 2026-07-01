package com.hana.exchange.market.application;

import org.springframework.stereotype.Service;

import com.hana.exchange.market.client.OmniLensMarketRealtimeSubscriptionClient;
import com.hana.exchange.market.client.OmniLensRealtimeSubscriptionResponse;

@Service
public class MarketRealtimeSubscriptionService {

	private final OmniLensMarketRealtimeSubscriptionClient subscriptionClient;

	public MarketRealtimeSubscriptionService(OmniLensMarketRealtimeSubscriptionClient subscriptionClient) {
		this.subscriptionClient = subscriptionClient;
	}

	public OmniLensRealtimeSubscriptionResponse subscribe(String stockCode, String session) {
		return subscriptionClient.subscribe(stockCode, session);
	}

	public OmniLensRealtimeSubscriptionResponse unsubscribe(String stockCode, String session) {
		return subscriptionClient.unsubscribe(stockCode, session);
	}
}
