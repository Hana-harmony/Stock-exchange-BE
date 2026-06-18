package com.hana.exchange.market.client;

public interface OmniLensMarketQuoteClient {

	OmniLensMarketQuote getQuote(String stockCode, String currency);
}
