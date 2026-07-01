package com.hana.exchange.market.application;

import java.util.List;

public interface MarketQuoteRealtimeSubscriber {

	void requestSubscription(List<String> stockCodes, String currency);
}
