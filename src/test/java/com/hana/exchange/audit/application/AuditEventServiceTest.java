package com.hana.exchange.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.audit.domain.AuditEvent;
import com.hana.exchange.audit.domain.AuditEventType;
import com.hana.exchange.config.AuditProperties;

class AuditEventServiceTest {

	private final InMemoryAuditEventRepository auditEventRepository = new InMemoryAuditEventRepository();
	private final AuditEventService auditEventService = new AuditEventService(
			auditEventRepository,
			null,
			new IdGenerator(),
			new AuditEventMasker(),
			new AuditProperties(365, false, null));

	@Test
	void recordMasksSensitiveSummaryAndSubjectIdBeforePersistingAuditEvent() {
		auditEventService.record(
				"ACC-AUDITSVC01",
				"USR-AUDITSVC01",
				AuditEventType.TAX_REFUND_CASE_UPSERTED,
				"TAX_REFUND_CASE",
				"abcdefghijklmnopqrstuvwxyz123456",
				"email=test@example.com phone=010-1234-5678 rrn=900101-1234567",
				Instant.parse("2026-06-18T06:00:00Z"));

		List<AuditEvent> events = auditEventRepository.findByAccountId("ACC-AUDITSVC01", 10);

		assertThat(events).hasSize(1);
		assertThat(events.get(0).subjectId()).isEqualTo("[MASKED_SECRET]");
		assertThat(events.get(0).summary()).contains("[MASKED_EMAIL]", "[MASKED_PHONE]", "[MASKED_RRN]");
	}

	@Test
	void purgeExpiredEventsDeletesEventsOlderThanRetentionDays() {
		Instant now = Instant.now();
		auditEventRepository.save(event("AUD-AUDITSVC01", "ACC-AUDITSVC02", now.minus(java.time.Duration.ofDays(366))));
		auditEventRepository.save(event("AUD-AUDITSVC02", "ACC-AUDITSVC02", now.minus(java.time.Duration.ofDays(10))));

		int deletedCount = auditEventService.purgeExpiredEvents();

		assertThat(deletedCount).isEqualTo(1);
		assertThat(auditEventRepository.findByAccountId("ACC-AUDITSVC02", 10))
				.extracting(AuditEvent::auditEventId)
				.containsExactly("AUD-AUDITSVC02");
	}

	private AuditEvent event(String auditEventId, String accountId, Instant occurredAt) {
		return new AuditEvent(
				auditEventId,
				accountId,
				"USR-AUDITSVC02",
				AuditEventType.TRADE_EXECUTED,
				"TRADE",
				"TRD-AUDITSVC02",
				"BUY 1 005930",
				occurredAt);
	}
}
