package com.hana.exchange.market.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.application.MarketQuoteStreamPublisher;
import com.hana.exchange.market.domain.MarketQuoteStreamProcessingResult;
import com.hana.exchange.market.domain.MarketQuoteStreamProcessingStats;
import com.hana.exchange.market.domain.MarketQuoteStreamPublishResponse;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

class OmniLensMarketQuoteStreamMessageHandlerTest {

	@Test
	void acceptBuffersAndDrainsValidHanaQuoteTick() {
		MarketQuoteStreamPublisher publisher = mock(MarketQuoteStreamPublisher.class);
		when(publisher.publish(any(MarketQuoteTickRequest.class)))
				.thenReturn(new MarketQuoteStreamPublishResponse("005930", "KOSPI", 3, java.util.List.of(), null));
		OmniLensMarketQuoteStreamMessageHandler handler = handler(publisher, 10);

		MarketQuoteStreamProcessingResult result = handler.accept(tickPayload("005930", "KOSPI"));
		int published = handler.drainBufferedTicks();
		MarketQuoteStreamProcessingStats stats = handler.stats();

		assertThat(result.accepted()).isTrue();
		assertThat(published).isEqualTo(1);
		assertThat(stats.acceptedCount()).isEqualTo(1);
		assertThat(stats.publishedCount()).isEqualTo(1);
		assertThat(stats.rejectedCount()).isZero();
		assertThat(stats.droppedCount()).isZero();
		assertThat(stats.bufferDepth()).isZero();
		assertThat(stats.lastMarketDataTime()).isNotNull();
		verify(publisher).publish(any(MarketQuoteTickRequest.class));
	}

	@Test
	void acceptRejectsInvalidPayloadBeforePublish() {
		MarketQuoteStreamPublisher publisher = mock(MarketQuoteStreamPublisher.class);
		OmniLensMarketQuoteStreamMessageHandler handler = handler(publisher, 10);

		MarketQuoteStreamProcessingResult result = handler.accept(tickPayload("ABCDEF", "kospi"));
		int published = handler.drainBufferedTicks();

		assertThat(result.accepted()).isFalse();
		assertThat(result.status()).isEqualTo("REJECTED");
		assertThat(published).isZero();
		assertThat(handler.stats().rejectedCount()).isEqualTo(1);
		verifyNoInteractions(publisher);
	}

	@Test
	void acceptDropsWhenBackpressureBufferIsFull() {
		MarketQuoteStreamPublisher publisher = mock(MarketQuoteStreamPublisher.class);
		OmniLensMarketQuoteStreamMessageHandler handler = handler(publisher, 1);

		MarketQuoteStreamProcessingResult first = handler.accept(tickPayload("005930", "KOSPI"));
		MarketQuoteStreamProcessingResult second = handler.accept(tickPayload("000660", "KOSPI"));

		assertThat(first.accepted()).isTrue();
		assertThat(second.accepted()).isFalse();
		assertThat(second.status()).isEqualTo("DROPPED");
		assertThat(handler.stats().acceptedCount()).isEqualTo(1);
		assertThat(handler.stats().droppedCount()).isEqualTo(1);
	}

	private OmniLensMarketQuoteStreamMessageHandler handler(MarketQuoteStreamPublisher publisher, int bufferSize) {
		return new OmniLensMarketQuoteStreamMessageHandler(
				objectMapper(),
				validator(),
				publisher,
				new ExchangeBackendProperties(
						"http://localhost:8080",
						"",
						Duration.ofSeconds(3),
						Duration.ofSeconds(30),
						new ExchangeBackendProperties.Stream(
								false,
								"/ws/market/quotes",
								"USD",
								true,
								Duration.ofSeconds(1),
								Duration.ofSeconds(30),
								bufferSize,
								Duration.ofMillis(50))));
	}

	private ObjectMapper objectMapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}

	private Validator validator() {
		return Validation.buildDefaultValidatorFactory().getValidator();
	}

	private String tickPayload(String stockCode, String market) {
		return """
				{
				  "stockCode": "%s",
				  "stockName": "Samsung Electronics",
				  "market": "%s",
				  "currentPriceKrw": 75000,
				  "changeRate": 1.25,
				  "volume": 1000000,
				  "localCurrency": "USD",
				  "localCurrencyPrice": 54.00,
				  "fxRate": 0.00072,
				  "fxRateTime": "2026-06-18T06:00:00Z",
				  "fxStale": false,
				  "marketDataTime": "2026-06-18T06:00:01Z",
				  "source": "HANA_OMNILENS_API_STREAM"
				}
				""".formatted(stockCode, market);
	}
}
