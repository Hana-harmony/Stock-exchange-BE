package com.hana.exchange.market.client;

public interface OmniLensOrderBookClient {

	OmniLensOrderBookResponse getOrderBook(String stockCode, String currency);
}
