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
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;
import com.hana.exchange.market.application.MarketQuoteCache.CachedQuotes;

@Service
public class MarketQuoteService {

	private static final List<String> ALL_KOREAN_STOCKS_CACHE_KEY = List.of("__ALL_KOREAN_STOCKS__");

	private final OmniLensMarketQuoteClient omniLensMarketQuoteClient;
	private final MarketQuoteCache marketQuoteCache;
	private final ExchangeBackendProperties properties;

	public MarketQuoteService(
			OmniLensMarketQuoteClient omniLensMarketQuoteClient,
			MarketQuoteCache marketQuoteCache,
			ExchangeBackendProperties properties) {
		this.omniLensMarketQuoteClient = omniLensMarketQuoteClient;
		this.marketQuoteCache = marketQuoteCache;
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
		boolean allKoreanStocks = useDefaultUniverse && (stockCodes == null || stockCodes.isEmpty());
		List<String> resolvedStockCodes = allKoreanStocks ? List.of() : resolveStockCodes(stockCodes);
		CachedQuotes cachedQuotes = allKoreanStocks
				? getAllQuotesWithCache(currency)
				: resolvedStockCodes.isEmpty()
				? emptyQuotes()
				: getQuotesWithCache(resolvedStockCodes, currency);
		String normalizedMarket = normalizeMarket(market);
		List<MarketQuoteSnapshot.Quote> exchangeQuotes = cachedQuotes.quotes().stream()
				.filter(quote -> normalizedMarket == null || normalizedMarket.equals(quote.market()))
				.map(quote -> toExchangeQuote(quote, cachedQuotes.stale()))
				.toList();
		return new MarketQuoteSnapshot(
				dataSource(cachedQuotes.quotes()),
				marketCoverage,
				"en",
				currency,
				"EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING",
				new MarketQuoteSnapshot.Transport("REST", "WebSocket"),
				normalizedMarket,
				toCacheMetadata(cachedQuotes),
				exchangeQuotes.size(),
				exchangeQuotes,
				Instant.now());
	}

	public MarketQuoteSnapshot getQuoteSnapshot(String stockCode, String currency) {
		CachedQuotes cachedQuotes = getQuoteWithCache(stockCode, currency);
		OmniLensMarketQuote quote = cachedQuotes.quotes().get(0);
		return new MarketQuoteSnapshot(
				quote.source(),
				quote.stockCode(),
				"en",
				quote.localCurrency(),
				"EXCHANGE_MOCK_LEDGER_NOT_KIS_MOCK_TRADING",
				new MarketQuoteSnapshot.Transport("REST", "WebSocket"),
				quote.market(),
				toCacheMetadata(cachedQuotes),
				1,
				List.of(toExchangeQuote(quote, cachedQuotes.stale())),
				Instant.now());
	}

	private CachedQuotes getQuotesWithCache(List<String> stockCodes, String currency) {
		return marketQuoteCache.getFresh(stockCodes, currency)
				.orElseGet(() -> fetchQuotesWithStaleFallback(stockCodes, currency));
	}

	private CachedQuotes getAllQuotesWithCache(String currency) {
		return marketQuoteCache.getFresh(ALL_KOREAN_STOCKS_CACHE_KEY, currency)
				.orElseGet(() -> fetchAllQuotesWithStaleFallback(currency));
	}

	private CachedQuotes getQuoteWithCache(String stockCode, String currency) {
		List<String> stockCodes = List.of(stockCode);
		return marketQuoteCache.getFresh(stockCodes, currency)
				.orElseGet(() -> fetchQuoteWithStaleFallback(stockCode, currency));
	}

	private CachedQuotes fetchQuotesWithStaleFallback(List<String> stockCodes, String currency) {
		try {
			return marketQuoteCache.put(stockCodes, currency, omniLensMarketQuoteClient.getQuotes(stockCodes, currency));
		} catch (BusinessException exception) {
			return marketQuoteCache.getStale(stockCodes, currency)
					.orElseThrow(() -> exception);
		}
	}

	private CachedQuotes fetchAllQuotesWithStaleFallback(String currency) {
		try {
			return marketQuoteCache.put(ALL_KOREAN_STOCKS_CACHE_KEY, currency, omniLensMarketQuoteClient.getAllQuotes(currency));
		} catch (BusinessException exception) {
			return marketQuoteCache.getStale(ALL_KOREAN_STOCKS_CACHE_KEY, currency)
					.orElseThrow(() -> exception);
		}
	}

	private CachedQuotes fetchQuoteWithStaleFallback(String stockCode, String currency) {
		List<String> stockCodes = List.of(stockCode);
		try {
			return marketQuoteCache.put(stockCodes, currency, List.of(omniLensMarketQuoteClient.getQuote(stockCode, currency)));
		} catch (BusinessException exception) {
			return marketQuoteCache.getStale(stockCodes, currency)
					.orElseThrow(() -> exception);
		}
	}

	private CachedQuotes emptyQuotes() {
		return new CachedQuotes(List.of(), MarketQuoteCache.CacheStatus.EMPTY, null, null, null);
	}

	private MarketQuoteSnapshot.Cache toCacheMetadata(CachedQuotes cachedQuotes) {
		return new MarketQuoteSnapshot.Cache(
				cachedQuotes.status().name(),
				cachedQuotes.cachedAt(),
				cachedQuotes.expiresAt(),
				cachedQuotes.staleUntil());
	}

	private MarketQuoteSnapshot.Quote toExchangeQuote(OmniLensMarketQuote quote, boolean stale) {
		return new MarketQuoteSnapshot.Quote(
				quote.stockCode(),
				displayName(quote),
				quote.market(),
				toText(quote.currentPriceKrw()),
				toText(quote.changeRate()),
				quote.volume(),
				quote.localCurrency(),
				toText(quote.localCurrencyPrice()),
				fxRate(quote),
				fxRateTime(quote),
				fxRateSource(quote),
				stale || quote.fxStale());
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

	private List<String> resolveStockCodes(List<String> requestedStockCodes) {
		if (requestedStockCodes == null) {
			return List.of();
		}
		Set<String> deduplicated = new LinkedHashSet<>(requestedStockCodes);
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
			return "ALL_KOREAN_STOCKS";
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

	private String fxRate(OmniLensMarketQuote quote) {
		if (quote.fxRate() != null) {
			return toText(quote.fxRate());
		}
		return deriveFxRate(quote);
	}

	private Instant fxRateTime(OmniLensMarketQuote quote) {
		return quote.fxRateTime() == null ? quote.marketDataTime() : quote.fxRateTime();
	}

	private String fxRateSource(OmniLensMarketQuote quote) {
		if (quote.fxRateSource() != null && !quote.fxRateSource().isBlank()) {
			return quote.fxRateSource();
		}
		return quote.source();
	}
}
