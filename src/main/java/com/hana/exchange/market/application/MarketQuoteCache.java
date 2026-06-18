package com.hana.exchange.market.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.client.OmniLensMarketQuote;

@Component
public class MarketQuoteCache {

	private final ConcurrentMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();
	private final ExchangeBackendProperties properties;
	private final Clock clock;

	public MarketQuoteCache(ExchangeBackendProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	public Optional<CachedQuotes> getFresh(List<String> stockCodes, String currency) {
		CacheEntry entry = cache.get(CacheKey.of(stockCodes, currency));
		if (entry == null) {
			return Optional.empty();
		}
		Instant now = Instant.now(clock);
		if (now.isBefore(entry.expiresAt())) {
			return Optional.of(entry.toCachedQuotes(CacheStatus.FRESH_CACHE));
		}
		return Optional.empty();
	}

	public Optional<CachedQuotes> getStale(List<String> stockCodes, String currency) {
		CacheEntry entry = cache.get(CacheKey.of(stockCodes, currency));
		if (entry == null) {
			return Optional.empty();
		}
		Instant now = Instant.now(clock);
		if (now.isBefore(entry.staleUntil())) {
			return Optional.of(entry.toCachedQuotes(CacheStatus.STALE_CACHE));
		}
		cache.remove(CacheKey.of(stockCodes, currency), entry);
		return Optional.empty();
	}

	public CachedQuotes put(List<String> stockCodes, String currency, List<OmniLensMarketQuote> quotes) {
		Instant cachedAt = Instant.now(clock);
		Instant expiresAt = cachedAt.plus(properties.quoteCacheTtl());
		Instant staleUntil = expiresAt.plus(properties.quoteCacheStaleTtl());
		CacheEntry entry = new CacheEntry(List.copyOf(quotes), cachedAt, expiresAt, staleUntil);
		cache.put(CacheKey.of(stockCodes, currency), entry);
		return entry.toCachedQuotes(CacheStatus.LIVE);
	}

	public enum CacheStatus {
		LIVE,
		FRESH_CACHE,
		STALE_CACHE,
		EMPTY
	}

	public record CachedQuotes(
			List<OmniLensMarketQuote> quotes,
			CacheStatus status,
			Instant cachedAt,
			Instant expiresAt,
			Instant staleUntil
	) {
		boolean stale() {
			return status == CacheStatus.STALE_CACHE;
		}
	}

	private record CacheKey(List<String> stockCodes, String currency) {
		private static CacheKey of(List<String> stockCodes, String currency) {
			return new CacheKey(List.copyOf(stockCodes), currency);
		}
	}

	private record CacheEntry(
			List<OmniLensMarketQuote> quotes,
			Instant cachedAt,
			Instant expiresAt,
			Instant staleUntil
	) {
		private CachedQuotes toCachedQuotes(CacheStatus status) {
			return new CachedQuotes(quotes, status, cachedAt, expiresAt, staleUntil);
		}
	}
}
