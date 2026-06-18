package com.hana.exchange.audit.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "exchange.audit", name = "retention-worker-enabled", havingValue = "true")
public class AuditRetentionWorker {

	private final AuditEventService auditEventService;

	public AuditRetentionWorker(AuditEventService auditEventService) {
		this.auditEventService = auditEventService;
	}

	@Scheduled(fixedDelayString = "#{@auditProperties.retentionFixedDelay().toMillis()}")
	public void purgeExpiredEvents() {
		auditEventService.purgeExpiredEvents();
	}
}
