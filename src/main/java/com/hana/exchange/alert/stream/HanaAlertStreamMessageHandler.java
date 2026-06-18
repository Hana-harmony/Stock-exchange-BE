package com.hana.exchange.alert.stream;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hana.exchange.alert.application.AlertEventService;
import com.hana.exchange.alert.domain.AlertEventIngestRequest;
import com.hana.exchange.alert.domain.AlertStreamProcessingResult;
import com.hana.exchange.alert.domain.AlertStreamProcessingStats;
import com.hana.exchange.config.ExchangeBackendProperties;

@Component
public class HanaAlertStreamMessageHandler {

	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final AlertEventService alertEventService;
	private final BlockingQueue<AlertEventIngestRequest> buffer;
	private final AtomicLong acceptedCount = new AtomicLong();
	private final AtomicLong ingestedCount = new AtomicLong();
	private final AtomicLong rejectedCount = new AtomicLong();
	private final AtomicLong droppedCount = new AtomicLong();
	private final AtomicReference<Instant> lastPublishedAt = new AtomicReference<>();

	public HanaAlertStreamMessageHandler(
			ObjectMapper objectMapper,
			Validator validator,
			AlertEventService alertEventService,
			ExchangeBackendProperties properties) {
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.alertEventService = alertEventService;
		this.buffer = new ArrayBlockingQueue<>(properties.stream().backpressureBufferSize());
	}

	public AlertStreamProcessingResult accept(String payload) {
		AlertEventIngestRequest request;
		try {
			request = objectMapper.readValue(payload, AlertEventIngestRequest.class);
		} catch (IOException exception) {
			rejectedCount.incrementAndGet();
			return AlertStreamProcessingResult.rejected("INVALID_JSON");
		}

		Set<ConstraintViolation<AlertEventIngestRequest>> violations = validator.validate(request);
		if (!violations.isEmpty()) {
			rejectedCount.incrementAndGet();
			return AlertStreamProcessingResult.rejected(violationReason(violations));
		}
		if (!buffer.offer(request)) {
			droppedCount.incrementAndGet();
			return AlertStreamProcessingResult.dropped("BACKPRESSURE_BUFFER_FULL");
		}
		acceptedCount.incrementAndGet();
		return AlertStreamProcessingResult.acceptedResult();
	}

	public int drainBufferedEvents() {
		int ingested = 0;
		AlertEventIngestRequest request = buffer.poll();
		while (request != null) {
			alertEventService.ingest(request);
			ingestedCount.incrementAndGet();
			lastPublishedAt.accumulateAndGet(request.publishedAt(), this::latest);
			ingested++;
			request = buffer.poll();
		}
		return ingested;
	}

	public AlertStreamProcessingStats stats() {
		return new AlertStreamProcessingStats(
				acceptedCount.get(),
				ingestedCount.get(),
				rejectedCount.get(),
				droppedCount.get(),
				buffer.size(),
				lastPublishedAt.get());
	}

	public Instant replayAfter() {
		return lastPublishedAt.get();
	}

	private String violationReason(Set<ConstraintViolation<AlertEventIngestRequest>> violations) {
		return violations.stream()
				.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
				.sorted()
				.collect(Collectors.joining("; "));
	}

	private Instant latest(Instant current, Instant candidate) {
		return Comparator.nullsFirst(Comparator.<Instant>naturalOrder()).compare(current, candidate) >= 0
				? current
				: candidate;
	}
}
