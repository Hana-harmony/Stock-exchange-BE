package com.hana.exchange.alert.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hana.exchange.alert.application.AlertEventService;
import com.hana.exchange.alert.domain.AlertEventIngestRequest;
import com.hana.exchange.alert.domain.AlertEventMatchResponse;
import com.hana.exchange.alert.domain.AlertStreamProcessingResult;
import com.hana.exchange.alert.domain.AlertStreamProcessingStats;
import com.hana.exchange.config.ExchangeBackendProperties;

class HanaAlertStreamMessageHandlerTest {

	@Test
	void acceptBuffersAndDrainsValidAnalyzedAlertEvent() {
		AlertEventService alertEventService = mock(AlertEventService.class);
		when(alertEventService.ingest(any(AlertEventIngestRequest.class)))
				.thenReturn(matchResponse("ALERT-STREAM-01"));
		HanaAlertStreamMessageHandler handler = handler(alertEventService, 10);

		AlertStreamProcessingResult result = handler.accept(eventPayload("ALERT-STREAM-01", "alert-stream-key-01", "005930"));
		int ingested = handler.drainBufferedEvents();
		AlertStreamProcessingStats stats = handler.stats();

		assertThat(result.accepted()).isTrue();
		assertThat(ingested).isEqualTo(1);
		assertThat(stats.acceptedCount()).isEqualTo(1);
		assertThat(stats.ingestedCount()).isEqualTo(1);
		assertThat(stats.rejectedCount()).isZero();
		assertThat(stats.droppedCount()).isZero();
		assertThat(stats.bufferDepth()).isZero();
		assertThat(stats.lastPublishedAt()).isNotNull();
		verify(alertEventService).ingest(any(AlertEventIngestRequest.class));
	}

	@Test
	void acceptRejectsInvalidPayloadBeforeIngest() {
		AlertEventService alertEventService = mock(AlertEventService.class);
		HanaAlertStreamMessageHandler handler = handler(alertEventService, 10);

		AlertStreamProcessingResult result = handler.accept(eventPayload("ALERT-BAD-01", "alert-stream-key-02", "ABCDEF"));
		int ingested = handler.drainBufferedEvents();

		assertThat(result.accepted()).isFalse();
		assertThat(result.status()).isEqualTo("REJECTED");
		assertThat(ingested).isZero();
		assertThat(handler.stats().rejectedCount()).isEqualTo(1);
		verifyNoInteractions(alertEventService);
	}

	@Test
	void acceptDropsWhenBackpressureBufferIsFull() {
		AlertEventService alertEventService = mock(AlertEventService.class);
		HanaAlertStreamMessageHandler handler = handler(alertEventService, 1);

		AlertStreamProcessingResult first = handler.accept(eventPayload("ALERT-STREAM-02", "alert-stream-key-03", "005930"));
		AlertStreamProcessingResult second = handler.accept(eventPayload("ALERT-STREAM-03", "alert-stream-key-04", "000660"));

		assertThat(first.accepted()).isTrue();
		assertThat(second.accepted()).isFalse();
		assertThat(second.status()).isEqualTo("DROPPED");
		assertThat(handler.stats().acceptedCount()).isEqualTo(1);
		assertThat(handler.stats().droppedCount()).isEqualTo(1);
	}

	private HanaAlertStreamMessageHandler handler(AlertEventService alertEventService, int bufferSize) {
		return new HanaAlertStreamMessageHandler(
				objectMapper(),
				validator(),
				alertEventService,
				new ExchangeBackendProperties(
						"http://localhost:8080",
						"",
						Duration.ofSeconds(3),
						Duration.ofSeconds(30),
						ExchangeBackendProperties.Retry.defaults(),
						new ExchangeBackendProperties.Stream(
								false,
								"/ws/market/quotes",
								"USD",
								true,
								false,
								"/ws/alerts/events",
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

	private AlertEventMatchResponse matchResponse(String eventId) {
		return new AlertEventMatchResponse(
				eventId,
				"key",
				"NEWS",
				"005930",
				List.of(),
				"Samsung supply chain update",
				"Translated AI summary",
				"https://news.example.com/original",
				"POSITIVE",
				"HIGH",
				"MEDIUM",
				true,
				true,
				0,
				List.of(),
				null);
	}

	private String eventPayload(String eventId, String idempotencyKey, String stockCode) {
		return """
				{
				  "eventId": "%s",
				  "idempotencyKey": "%s",
				  "sourceType": "NEWS",
				  "title": "Samsung supply chain update",
				  "summary": "Translated AI summary for local investors",
				  "originalUrl": "https://news.example.com/original",
				  "stockCode": "%s",
				  "relatedStocks": [],
				  "sentiment": "POSITIVE",
				  "importance": "HIGH",
				  "riskLevel": "MEDIUM",
				  "watchlistTarget": true,
				  "holderTarget": true,
				  "publishedAt": "2026-06-18T06:00:00Z"
				}
				""".formatted(eventId, idempotencyKey, stockCode);
	}
}
