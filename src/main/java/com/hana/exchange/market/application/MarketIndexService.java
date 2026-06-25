package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hana.exchange.market.client.OmniLensMarketIndex;
import com.hana.exchange.market.client.OmniLensMarketIndexClient;
import com.hana.exchange.market.domain.MarketIndexSnapshot;

@Service
public class MarketIndexService {

	private final OmniLensMarketIndexClient marketIndexClient;
	private final Clock clock;

	@Autowired
	public MarketIndexService(OmniLensMarketIndexClient marketIndexClient) {
		this(marketIndexClient, Clock.systemUTC());
	}

	MarketIndexService(OmniLensMarketIndexClient marketIndexClient, Clock clock) {
		this.marketIndexClient = marketIndexClient;
		this.clock = clock;
	}

	public MarketIndexSnapshot getIndexSnapshot() {
		List<OmniLensMarketIndex> indices = marketIndexClient.getIndices();
		return new MarketIndexSnapshot(
				dataSource(indices),
				new MarketIndexSnapshot.Transport("REST", "WebSocket"),
				indices.size(),
				indices.stream().map(this::toIndex).toList(),
				Instant.now(clock));
	}

	MarketIndexSnapshot.Index toIndex(OmniLensMarketIndex index) {
		return new MarketIndexSnapshot.Index(
				index.indexCode(),
				index.indexName(),
				index.market(),
				format(index.currentValue()),
				index.changeSign(),
				format(index.changeValue()),
				format(index.changeRate()),
				index.accumulatedVolume(),
				index.accumulatedTradingValue(),
				format(index.openValue()),
				format(index.highValue()),
				format(index.lowValue()),
				index.marketDataTime(),
				index.source());
	}

	private String dataSource(List<OmniLensMarketIndex> indices) {
		if (indices.isEmpty()) {
			return "HANA_OMNILENS_KIS_INDEX_EMPTY";
		}
		return indices.stream()
				.map(OmniLensMarketIndex::source)
				.distinct()
				.sorted()
				.reduce((left, right) -> left + "+" + right)
				.orElse("HANA_OMNILENS_KIS_INDEX");
	}

	private String format(BigDecimal value) {
		return value == null ? "0" : value.stripTrailingZeros().toPlainString();
	}
}
