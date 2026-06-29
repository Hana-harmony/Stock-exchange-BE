package com.hana.exchange.market.client;

public interface OmniLensMarketRealtimeSubscriptionClient {

	OmniLensRealtimeSubscriptionResponse subscribe(String stockCode, String session);

	OmniLensRealtimeSubscriptionResponse unsubscribe(String stockCode, String session);
}
