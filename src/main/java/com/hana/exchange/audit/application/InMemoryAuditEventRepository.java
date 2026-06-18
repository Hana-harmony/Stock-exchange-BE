package com.hana.exchange.audit.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.audit.domain.AuditEvent;

@Repository
@Profile("memory")
public class InMemoryAuditEventRepository implements AuditEventRepository {

	private final Map<String, AuditEvent> eventsById = new ConcurrentHashMap<>();

	@Override
	public void save(AuditEvent event) {
		eventsById.put(event.auditEventId(), event);
	}

	@Override
	public List<AuditEvent> findByAccountId(String accountId, int limit) {
		return eventsById.values()
				.stream()
				.filter(event -> event.accountId().equals(accountId))
				.sorted(Comparator.comparing(AuditEvent::occurredAt).reversed())
				.limit(limit)
				.toList();
	}

	@Override
	public int deleteOccurredBefore(java.time.Instant cutoff) {
		List<String> expiredIds = eventsById.values()
				.stream()
				.filter(event -> event.occurredAt().isBefore(cutoff))
				.map(AuditEvent::auditEventId)
				.toList();
		expiredIds.forEach(eventsById::remove);
		return expiredIds.size();
	}
}
