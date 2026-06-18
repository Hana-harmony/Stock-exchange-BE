package com.hana.exchange.market.client;

import com.hana.exchange.trade.domain.TradeSide;

public interface OmniLensOrderabilityClient {

	OmniLensOrderabilityResponse checkOrderability(String stockCode, TradeSide side, long quantity);
}
