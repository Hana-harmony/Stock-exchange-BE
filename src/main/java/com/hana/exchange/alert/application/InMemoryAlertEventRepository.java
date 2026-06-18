package com.hana.exchange.alert.application;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;

@Repository
public class InMemoryAlertEventRepository implements AlertEventRepository {

	private final Map<String, AlertEventMatchResult> resultsByEventId = new ConcurrentHashMap<>();
	private final Map<String, AlertEventMatchResult> resultsByIdempotencyKey = new ConcurrentHashMap<>();

	@Override
	public Optional<AlertEventMatchResult> findByIdempotencyKey(String idempotencyKey) {
		return Optional.ofNullable(resultsByIdempotencyKey.get(idempotencyKey));
	}

	@Override
	public Optional<AlertEventMatchResult> findByEventId(String eventId) {
		return Optional.ofNullable(resultsByEventId.get(eventId));
	}

	@Override
	public void save(AlertEvent event, AlertEventMatchResult matchResult) {
		resultsByEventId.put(event.eventId(), matchResult);
		resultsByIdempotencyKey.put(event.idempotencyKey(), matchResult);
	}
}
