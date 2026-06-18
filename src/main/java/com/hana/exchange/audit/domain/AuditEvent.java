package com.hana.exchange.audit.domain;

import java.time.Instant;

public record AuditEvent(
		String auditEventId,
		String accountId,
		String userId,
		AuditEventType eventType,
		String subjectType,
		String subjectId,
		String summary,
		Instant occurredAt
) {
}
