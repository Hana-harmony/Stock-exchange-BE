package com.hana.exchange.market.application;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

@Service
public class MarketQuoteService {

	private final OmniLensMarketQuoteClient omniLensMarketQuoteClient;
	private final ExchangeBackendProperties properties;

	public MarketQuoteService(
			OmniLensMarketQuoteClient omniLensMarketQuoteClient,
			ExchangeBackendProperties properties) {
		this.omniLensMarketQuoteClient = omniLensMarketQuoteClient;
		this.properties = properties;
	}

	public MarketQuoteSnapshot getQuoteSnapshot() {
		return getQuoteSnapshot(null, null, "USD");
	}

	public MarketQuoteSnapshot getQuoteSnapshot(List<String> stockCodes, String market, String currency) {
		return getQuoteSnapshot(stockCodes, market, currency, marketCoverage(stockCodes), true);
	}

	public MarketQuoteSnapshot getQuoteSnapshotForScope(
			List<String> stockCodes,
			String market,
			String currency,
			String marketCoverage) {
		return getQuoteSnapshot(stockCodes, market, currency, marketCoverage, false);
	}

	private MarketQuoteSnapshot getQuoteSnapshot(
			List<String> stockCodes,
			String market,
			String currency,
			String marketCoverage,
			boolean useDefaultUniverse) {
		List<String> resolvedStockCodes = resolveStockCodes(stockCodes, useDefaultUniverse);
		List<OmniLensMarketQuote> quotes = resolvedStockCodes.isEmpty()
				? List.of()
				: omniLensMarketQuoteClient.getQuotes(resolvedStockCodes, currency);
		String normalizedMarket = normalizeMarket(market);
		List<MarketQuoteSnapshot.Quote> exchangeQuotes = quotes.stream()
				.filter(quote -> normalizedMarket == null || normalizedMarket.equals(quote.market()))
				.map(this::toExchangeQuote)
				.toList();
		return new MarketQuoteSnapshot(
				dataSource(quotes),
				marketCoverage,
				"en",
				currency,
				"EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING",
				new MarketQuoteSnapshot.Transport("REST", "WebSocket"),
				normalizedMarket,
				exchangeQuotes.size(),
				exchangeQuotes,
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
				quote.market(),
				1,
				List.of(toExchangeQuote(quote)),
				Instant.now());
	}

	private MarketQuoteSnapshot.Quote toExchangeQuote(OmniLensMarketQuote quote) {
		return new MarketQuoteSnapshot.Quote(
				quote.stockCode(),
				displayName(quote),
				quote.market(),
				toText(quote.currentPriceKrw()),
				toText(quote.changeRate()),
				quote.volume(),
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

	private List<String> resolveStockCodes(List<String> requestedStockCodes, boolean useDefaultUniverse) {
		List<String> source = useDefaultUniverse && (requestedStockCodes == null || requestedStockCodes.isEmpty())
				? properties.defaultStockCodes()
				: requestedStockCodes;
		if (source == null) {
			return List.of();
		}
		Set<String> deduplicated = new LinkedHashSet<>(source);
		return deduplicated.stream()
				.filter(StringUtils::hasText)
				.toList();
	}

	private String normalizeMarket(String market) {
		if (!StringUtils.hasText(market)) {
			return null;
		}
		return market.toUpperCase(Locale.ROOT);
	}

	private String marketCoverage(List<String> requestedStockCodes) {
		if (requestedStockCodes == null || requestedStockCodes.isEmpty()) {
			return "CONFIGURED_KOREAN_STOCK_UNIVERSE";
		}
		return "REQUESTED_STOCK_CODES";
	}

	private String dataSource(List<OmniLensMarketQuote> quotes) {
		if (quotes.isEmpty()) {
			return "HANA_OMNILENS_API";
		}
		return quotes.stream()
				.map(OmniLensMarketQuote::source)
				.distinct()
				.reduce((left, right) -> "MIXED_HANA_OMNILENS_API")
				.orElse("HANA_OMNILENS_API");
	}

	private String deriveFxRate(OmniLensMarketQuote quote) {
		if (quote.currentPriceKrw() == null
				|| quote.localCurrencyPrice() == null
				|| quote.currentPriceKrw().signum() == 0) {
			return null;
		}
		return quote.localCurrencyPrice()
				.divide(quote.currentPriceKrw(), 10, RoundingMode.HALF_UP)
				.stripTrailingZeros()
				.toPlainString();
	}
}
