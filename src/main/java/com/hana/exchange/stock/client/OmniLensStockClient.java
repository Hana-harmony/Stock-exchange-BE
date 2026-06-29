package com.hana.exchange.stock.client;

public interface OmniLensStockClient {

	OmniLensStockSearchResponse search(String query, String market, String currency, int limit);

	OmniLensStockDetailResponse getDetail(String stockCode, String currency);

	OmniLensGlobalPeerResponse getGlobalPeers(String stockCode);
}
