package com.hana.exchange.market.client;

import java.time.LocalDate;
import java.util.List;

public interface OmniLensMarketIntradayClient {

	List<OmniLensMarketIntradayPrice> getIntraday(
			String stockCode,
			LocalDate date,
			int limit);
}
