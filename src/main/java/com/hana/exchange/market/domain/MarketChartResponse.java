package com.hana.exchange.market.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MarketChartResponse(
		String dataSource,
		String stockCode,
		String interval,
		LocalDate from,
		LocalDate to,
		String baseCurrency,
		String displayCurrency,
		String userLanguage,
		int pointCount,
		List<MarketChartPointResponse> points,
		Instant servedAt
) {
}
