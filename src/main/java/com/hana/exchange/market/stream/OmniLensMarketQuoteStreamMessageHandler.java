package com.hana.exchange.market.stream;

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
import com.hana.exchange.config.ExchangeBackendProperties;
import com.hana.exchange.market.application.MarketQuoteStreamPublisher;
import com.hana.exchange.market.domain.MarketQuoteStreamProcessingResult;
import com.hana.exchange.market.domain.MarketQuoteStreamProcessingStats;
import com.hana.exchange.market.domain.MarketQuoteTickRequest;

@Component
public class OmniLensMarketQuoteStreamMessageHandler {

	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final MarketQuoteStreamPublisher publisher;
	private final BlockingQueue<MarketQuoteTickRequest> buffer;
	private final AtomicLong acceptedCount = new AtomicLong();
	private final AtomicLong publishedCount = new AtomicLong();
	private final AtomicLong rejectedCount = new AtomicLong();
	private final AtomicLong droppedCount = new AtomicLong();
	private final AtomicReference<Instant> lastMarketDataTime = new AtomicReference<>();

	public OmniLensMarketQuoteStreamMessageHandler(
			ObjectMapper objectMapper,
			Validator validator,
			MarketQuoteStreamPublisher publisher,
			ExchangeBackendProperties properties) {
		this.objectMapper = objectMapper;
		this.validator = validator;
		this.publisher = publisher;
		this.buffer = new ArrayBlockingQueue<>(properties.stream().backpressureBufferSize());
	}

	public MarketQuoteStreamProcessingResult accept(String payload) {
		MarketQuoteTickRequest request;
		try {
			request = objectMapper.readValue(payload, MarketQuoteTickRequest.class);
		} catch (IOException exception) {
			rejectedCount.incrementAndGet();
			return MarketQuoteStreamProcessingResult.rejected("INVALID_JSON");
		}

		Set<ConstraintViolation<MarketQuoteTickRequest>> violations = validator.validate(request);
		if (!violations.isEmpty()) {
			rejectedCount.incrementAndGet();
			return MarketQuoteStreamProcessingResult.rejected(violationReason(violations));
		}
		if (!buffer.offer(request)) {
			droppedCount.incrementAndGet();
			return MarketQuoteStreamProcessingResult.dropped("BACKPRESSURE_BUFFER_FULL");
		}
		acceptedCount.incrementAndGet();
		return MarketQuoteStreamProcessingResult.acceptedResult();
	}

	public int drainBufferedTicks() {
		int published = 0;
		MarketQuoteTickRequest request = buffer.poll();
		while (request != null) {
			publisher.publish(request);
			publishedCount.incrementAndGet();
			lastMarketDataTime.accumulateAndGet(request.marketDataTime(), this::latest);
			published++;
			request = buffer.poll();
		}
		return published;
	}

	public MarketQuoteStreamProcessingStats stats() {
		return new MarketQuoteStreamProcessingStats(
				acceptedCount.get(),
				publishedCount.get(),
				rejectedCount.get(),
				droppedCount.get(),
				buffer.size(),
				lastMarketDataTime.get());
	}

	public Instant replayAfter() {
		return lastMarketDataTime.get();
	}

	private String violationReason(Set<ConstraintViolation<MarketQuoteTickRequest>> violations) {
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
