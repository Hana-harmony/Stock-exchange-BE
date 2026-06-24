package com.hana.exchange.market.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.TaskScheduler;

import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.application.MarketQuoteStreamPublisher;
import com.hana.exchange.market.client.OmniLensMarketQuote;
import com.hana.exchange.market.client.OmniLensMarketQuoteClient;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

class OmniLensMarketQuoteSnapshotRelayTest {

	@Test
	void relayOncePublishesConfiguredQuoteSnapshotToWebSocketTopics() {
		OmniLensMarketQuoteClient quoteClient = mock(OmniLensMarketQuoteClient.class);
		MarketQuoteStreamPublisher publisher = mock(MarketQuoteStreamPublisher.class);
		OmniLensMarketQuoteSnapshotRelay relay = new OmniLensMarketQuoteSnapshotRelay(
				quoteClient,
				publisher,
				properties(),
				mock(TaskScheduler.class),
				environment());
		when(quoteClient.getQuotes(List.of("005930", "000660"), "USD")).thenReturn(List.of(
				quote("005930", "Samsung Electronics"),
				quote("000660", "SK hynix")));

		int published = relay.relayOnce();

		ArgumentCaptor<MarketQuoteTickRequest> captor = ArgumentCaptor.forClass(MarketQuoteTickRequest.class);
		verify(publisher, times(2)).publish(captor.capture());
		MarketQuoteTickRequest tick = captor.getAllValues().get(0);
		assertThat(published).isEqualTo(2);
		assertThat(tick.stockCode()).isEqualTo("005930");
		assertThat(tick.stockName()).isEqualTo("삼성전자");
		assertThat(tick.stockNameEn()).isEqualTo("Samsung Electronics");
		assertThat(tick.market()).isEqualTo("KOSPI");
		assertThat(tick.currentPriceKrw()).isEqualByComparingTo("325500");
		assertThat(tick.localCurrency()).isEqualTo("USD");
		assertThat(tick.fxRate()).isEqualByComparingTo("0.00065");
		assertThat(tick.source()).isEqualTo("KIS_WEBSOCKET_TRADE+STOCK_EXCHANGE_WS_RELAY");
	}

	private ExchangeBackendProperties properties() {
		return new ExchangeBackendProperties(
				"http://localhost:8080",
				"",
				Duration.ofSeconds(3),
				Duration.ofSeconds(30),
				ExchangeBackendProperties.Retry.defaults(),
				new ExchangeBackendProperties.Stream(
						true,
						"/ws/market/quotes",
						"USD",
						true,
						false,
						"/ws/alerts/events",
						true,
						Duration.ofSeconds(1),
						Duration.ofSeconds(30),
						1000,
						Duration.ofMillis(50)));
	}

	private MockEnvironment environment() {
		return new MockEnvironment()
				.withProperty("HANA_OMNILENS_QUOTE_SNAPSHOT_RELAY_ENABLED", "true")
				.withProperty("HANA_OMNILENS_QUOTE_RELAY_STOCK_CODES", "005930,000660")
				.withProperty("HANA_OMNILENS_QUOTE_STREAM_CURRENCY", "USD")
				.withProperty("HANA_OMNILENS_QUOTE_RELAY_INTERVAL", "2s");
	}

	private OmniLensMarketQuote quote(String stockCode, String stockNameEn) {
		return new OmniLensMarketQuote(
				stockCode,
				"삼성전자",
				stockNameEn,
				"KOSPI",
				new BigDecimal("325500"),
				new BigDecimal("5.00"),
				22004111L,
				new BigDecimal("325500"),
				"KRW",
				new BigDecimal("211.5750"),
				"USD",
				new BigDecimal("0.00065"),
				Instant.parse("2026-06-24T02:45:44Z"),
				"EXCHANGE_RATE_CACHE",
				false,
				2773061323L,
				new BigDecimal("47.4329"),
				new BigDecimal("47.4300"),
				LocalDate.parse("2026-06-23"),
				Instant.parse("2026-06-24T02:49:13Z"),
				"KIS_WEBSOCKET_TRADE");
	}
}
