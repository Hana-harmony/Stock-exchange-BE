package com.hana.exchange.market.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

@Service
public class MarketQuoteService {

	private final OmniLensMarketQuoteClient omniLensMarketQuoteClient;

	public MarketQuoteService(OmniLensMarketQuoteClient omniLensMarketQuoteClient) {
		this.omniLensMarketQuoteClient = omniLensMarketQuoteClient;
	}

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

	public MarketQuoteSnapshot getQuoteSnapshot(String stockCode, String currency) {
		OmniLensMarketQuote quote = omniLensMarketQuoteClient.getQuote(stockCode, currency);
		return new MarketQuoteSnapshot(
				quote.source(),
				quote.stockCode(),
				"en",
				quote.localCurrency(),
				"EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING",
				new MarketQuoteSnapshot.Transport("REST", "WebSocket"),
				List.of(toExchangeQuote(quote)),
				Instant.now());
	}

	private MarketQuoteSnapshot.Quote toExchangeQuote(OmniLensMarketQuote quote) {
		return new MarketQuoteSnapshot.Quote(
				quote.stockCode(),
				displayName(quote),
				toText(quote.currentPriceKrw()),
				quote.localCurrency(),
				toText(quote.localCurrencyPrice()),
				deriveFxRate(quote),
				quote.marketDataTime(),
				false);
	}

	private String displayName(OmniLensMarketQuote quote) {
		if (quote.stockNameEn() != null && !quote.stockNameEn().isBlank()) {
			return quote.stockNameEn();
		}
		return quote.stockName();
	}

	private String toText(java.math.BigDecimal value) {
		return value == null ? null : value.stripTrailingZeros().toPlainString();
	}

	private String deriveFxRate(OmniLensMarketQuote quote) {
		if (quote.currentPriceKrw() == null
				|| quote.localCurrencyPrice() == null
				|| quote.currentPriceKrw().signum() == 0) {
			return null;
		}
		return quote.localCurrencyPrice()
				.divide(quote.currentPriceKrw(), 10, java.math.RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}
}
