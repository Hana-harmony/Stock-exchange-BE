package com.hana.exchange.alert.stream;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

	private static final int MAX_INGEST_ATTEMPTS = 3;

	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final AlertEventService alertEventService;
	private final BlockingQueue<BufferedAlertEvent> buffer;
	private final AtomicLong acceptedCount = new AtomicLong();
	private final AtomicLong ingestedCount = new AtomicLong();
	private final AtomicLong rejectedCount = new AtomicLong();
	private final AtomicLong droppedCount = new AtomicLong();
	private final AtomicLong retryScheduledCount = new AtomicLong();
	private final AtomicLong failedIngestCount = new AtomicLong();
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
		if (!buffer.offer(new BufferedAlertEvent(request, 1))) {
			droppedCount.incrementAndGet();
			return AlertStreamProcessingResult.dropped("BACKPRESSURE_BUFFER_FULL");
		}
		acceptedCount.incrementAndGet();
		return AlertStreamProcessingResult.acceptedResult();
	}

	public int drainBufferedEvents() {
		int ingested = 0;
		List<BufferedAlertEvent> retryEvents = new ArrayList<>();
		BufferedAlertEvent event = buffer.poll();
		while (event != null) {
			AlertEventIngestRequest request = event.request();
			try {
				alertEventService.ingest(request);
			} catch (RuntimeException exception) {
				failedIngestCount.incrementAndGet();
				if (event.attemptCount() < MAX_INGEST_ATTEMPTS) {
					retryEvents.add(event.nextAttempt());
					retryScheduledCount.incrementAndGet();
				} else {
					droppedCount.incrementAndGet();
				}
				event = buffer.poll();
				continue;
			}
			ingestedCount.incrementAndGet();
			lastPublishedAt.accumulateAndGet(request.publishedAt(), this::latest);
			ingested++;
			event = buffer.poll();
		}
		requeueRetryEvents(retryEvents);
		return ingested;
	}

	public AlertStreamProcessingStats stats() {
		return new AlertStreamProcessingStats(
				acceptedCount.get(),
				ingestedCount.get(),
				rejectedCount.get(),
				droppedCount.get(),
				retryScheduledCount.get(),
				failedIngestCount.get(),
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

	private void requeueRetryEvents(List<BufferedAlertEvent> retryEvents) {
		for (BufferedAlertEvent retryEvent : retryEvents) {
			if (!buffer.offer(retryEvent)) {
				droppedCount.incrementAndGet();
			}
		}
	}

	private record BufferedAlertEvent(AlertEventIngestRequest request, int attemptCount) {

		private BufferedAlertEvent nextAttempt() {
			return new BufferedAlertEvent(request, attemptCount + 1);
		}
	}
}
