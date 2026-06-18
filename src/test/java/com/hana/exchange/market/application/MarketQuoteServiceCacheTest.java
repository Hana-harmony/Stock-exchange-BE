package com.hana.exchange.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteSnapshot;

class MarketQuoteServiceCacheTest {

	@Test
	void quoteListUsesFreshCacheBeforeCallingHanaAgain() {
		MutableClock clock = new MutableClock(Instant.parse("2026-06-18T06:00:00Z"));
		FakeQuoteClient quoteClient = new FakeQuoteClient();
		quoteClient.quotes = List.of(quote("005930", "54.00"));
		MarketQuoteService service = service(quoteClient, clock);

		MarketQuoteSnapshot firstSnapshot = service.getQuoteSnapshot(List.of("005930"), null, "USD");
		quoteClient.quotes = List.of(quote("005930", "99.00"));
		MarketQuoteSnapshot secondSnapshot = service.getQuoteSnapshot(List.of("005930"), null, "USD");

		assertThat(firstSnapshot.cache().status()).isEqualTo("LIVE");
		assertThat(secondSnapshot.cache().status()).isEqualTo("FRESH_CACHE");
		assertThat(secondSnapshot.quotes().get(0).localCurrencyPrice()).isEqualTo("54");
		assertThat(secondSnapshot.quotes().get(0).fxStale()).isFalse();
		assertThat(quoteClient.getQuotesCallCount).isEqualTo(1);
	}

	@Test
	void quoteListFallsBackToStaleCacheWhenHanaUnavailable() {
		MutableClock clock = new MutableClock(Instant.parse("2026-06-18T06:00:00Z"));
		FakeQuoteClient quoteClient = new FakeQuoteClient();
		quoteClient.quotes = List.of(quote("005930", "54.00"));
		MarketQuoteService service = service(quoteClient, clock);

		service.getQuoteSnapshot(List.of("005930"), null, "USD");
		clock.advance(Duration.ofSeconds(4));
		quoteClient.failQuotes = true;
		MarketQuoteSnapshot staleSnapshot = service.getQuoteSnapshot(List.of("005930"), null, "USD");

		assertThat(staleSnapshot.cache().status()).isEqualTo("STALE_CACHE");
		assertThat(staleSnapshot.quotes().get(0).fxStale()).isTrue();
		assertThat(staleSnapshot.quotes().get(0).localCurrencyPrice()).isEqualTo("54");
		assertThat(quoteClient.getQuotesCallCount).isEqualTo(2);

		clock.advance(Duration.ofSeconds(31));
		assertThatThrownBy(() -> service.getQuoteSnapshot(List.of("005930"), null, "USD"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
	}

	private MarketQuoteService service(FakeQuoteClient quoteClient, Clock clock) {
		ExchangeBackendProperties properties = new ExchangeBackendProperties(
				"http://localhost:8080",
				"",
				List.of("005930"),
				Duration.ofSeconds(3),
				Duration.ofSeconds(30));
		return new MarketQuoteService(
				quoteClient,
				new MarketQuoteCache(properties, clock),
				properties);
	}

	private static OmniLensMarketQuote quote(String stockCode, String usdPrice) {
		return new OmniLensMarketQuote(
				stockCode,
				"종목명",
				"Samsung Electronics",
				"KOSPI",
				new BigDecimal("75000"),
				new BigDecimal("1.25"),
				1000000L,
				new BigDecimal("75000"),
				"KRW",
				new BigDecimal(usdPrice),
				"USD",
				50000000L,
				new BigDecimal("54.5"),
				new BigDecimal("72.3"),
				LocalDate.parse("2026-06-18"),
				Instant.parse("2026-06-18T06:00:00Z"),
				"HANA_OMNILENS_API");
	}

	private static class FakeQuoteClient implements OmniLensMarketQuoteClient {

		private List<OmniLensMarketQuote> quotes = List.of();
		private boolean failQuotes;
		private int getQuotesCallCount;

		@Override
		public OmniLensMarketQuote getQuote(String stockCode, String currency) {
			throw new UnsupportedOperationException("single quote is not used in this test");
		}

		@Override
		public List<OmniLensMarketQuote> getQuotes(List<String> stockCodes, String currency) {
			getQuotesCallCount++;
			if (failQuotes) {
				throw new BusinessException(ErrorCode.MARKET_UPSTREAM_UNAVAILABLE);
			}
			return quotes;
		}
	}

	private static class MutableClock extends Clock {

		private Instant instant;

		MutableClock(Instant instant) {
			this.instant = instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneId.of("UTC");
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}
	}
}
