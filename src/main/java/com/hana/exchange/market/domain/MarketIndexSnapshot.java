package com.hana.exchange.market.domain;

import java.time.Instant;
import java.util.List;

public record MarketIndexSnapshot(
		String dataSource,
		Transport transport,
		int indexCount,
		List<Index> indices,
		Instant servedAt
) {
	public record Transport(String snapshot, String realtime) {
	}

	public record Index(
			String indexCode,
			String indexName,
			String market,
			String currentValue,
			String changeSign,
			String changeValue,
			String changeRate,
			long accumulatedVolume,
			long accumulatedTradingValue,
			String openValue,
			String highValue,
			String lowValue,
			Instant marketDataTime,
			String source
	) {
	}
}
