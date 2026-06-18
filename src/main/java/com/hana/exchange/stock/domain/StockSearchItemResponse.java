package com.hana.exchange.stock.domain;

public record StockSearchItemResponse(
		String stockCode,
		String stockName,
		String market,
		String sector,
		String dataSource
) {
}
