package com.hana.exchange.audit.domain;

import java.time.Instant;

public record AuditEventResponse(
		String auditEventId,
		AuditEventType eventType,
		String subjectType,
		String subjectId,
		String summary,
		Instant occurredAt
) {
}
