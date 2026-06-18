package com.hana.exchange.market.client;

import java.util.List;

public interface OmniLensMarketQuoteClient {

	OmniLensMarketQuote getQuote(String stockCode, String currency);

	default List<OmniLensMarketQuote> getQuotes(List<String> stockCodes, String currency) {
		return stockCodes.stream()
				.map(stockCode -> getQuote(stockCode, currency))
				.toList();
	}
}
