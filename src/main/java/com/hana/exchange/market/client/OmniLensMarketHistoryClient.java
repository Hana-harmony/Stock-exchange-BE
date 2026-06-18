package com.hana.exchange.market.client;

import java.time.LocalDate;

public interface OmniLensMarketHistoryClient {

	OmniLensMarketHistoryResponse getHistory(
			String stockCode,
			LocalDate from,
			LocalDate to,
			String interval,
			String currency);
}
