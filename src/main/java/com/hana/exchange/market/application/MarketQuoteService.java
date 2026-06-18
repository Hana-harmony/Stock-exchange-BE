package com.hana.exchange.market.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.market.domain.MarketQuoteSnapshot;

@Service
public class MarketQuoteService {

	public MarketQuoteSnapshot getQuoteSnapshot() {
		return new MarketQuoteSnapshot(
				"HANA_OMNILENS_API_PLANNED",
				"ALL_KOREAN_LISTED_STOCKS",
				"en",
				"USD",
				"EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING",
				new MarketQuoteSnapshot.Transport("REST", "WebSocket"),
				List.of(),
				Instant.now());
	}
}
