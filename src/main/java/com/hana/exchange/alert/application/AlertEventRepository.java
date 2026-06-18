package com.hana.exchange.alert.application;

import java.util.Optional;

import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;

public interface AlertEventRepository {

	Optional<AlertEventMatchResult> findByIdempotencyKey(String idempotencyKey);

	Optional<AlertEventMatchResult> findByEventId(String eventId);

	void save(AlertEvent event, AlertEventMatchResult matchResult);
}
