package com.hana.exchange.market.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.market.client.OmniLensOrderBookClient;
import com.hana.exchange.market.client.OmniLensOrderBookLevel;
import com.hana.exchange.market.client.OmniLensOrderBookResponse;
import com.hana.exchange.market.domain.MarketOrderBookResponse;

@Service
public class MarketOrderBookService {

	private final OmniLensOrderBookClient omniLensOrderBookClient;

	public MarketOrderBookService(OmniLensOrderBookClient omniLensOrderBookClient) {
		this.omniLensOrderBookClient = omniLensOrderBookClient;
	}

	public MarketOrderBookResponse getOrderBook(String stockCode, String currency) {
		OmniLensOrderBookResponse orderBook = omniLensOrderBookClient.getOrderBook(stockCode, currency);
		return new MarketOrderBookResponse(
				orderBook.source(),
				orderBook.stockCode(),
				orderBook.market(),
				orderBook.baseCurrency(),
				orderBook.localCurrency(),
				levels(orderBook.asks()),
				levels(orderBook.bids()),
				orderBook.marketDataTime(),
				Instant.now());
	}

	private List<MarketOrderBookResponse.Level> levels(List<OmniLensOrderBookLevel> levels) {
		if (levels == null) {
			return List.of();
		}
		return levels.stream()
				.map(level -> new MarketOrderBookResponse.Level(
						text(level.priceKrw()),
						text(level.localCurrencyPrice()),
						level.quantity(),
						level.orderCount()))
				.toList();
	}

	private String text(BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}
}
