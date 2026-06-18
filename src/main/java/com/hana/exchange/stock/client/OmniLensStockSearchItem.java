package com.hana.exchange.stock.client;

public record OmniLensStockSearchItem(
		String stockCode,
		String stockName,
		String stockNameEn,
		String market,
		String sector,
		String source
) {
}
