package com.hana.exchange.audit.domain;

import java.time.Instant;
import java.util.List;

public record AuditEventListResponse(
		String accountId,
		int eventCount,
		List<AuditEventResponse> events,
		Instant servedAt
) {
}
