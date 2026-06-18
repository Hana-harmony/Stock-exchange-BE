package com.hana.exchange.audit.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.application.IdGenerator;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.audit.domain.AuditEvent;
import com.hana.exchange.audit.domain.AuditEventListResponse;
import com.hana.exchange.audit.domain.AuditEventResponse;
import com.hana.exchange.audit.domain.AuditEventType;
import com.hana.exchange.common.exception.BusinessException;
import com.hana.exchange.common.exception.ErrorCode;
import com.hana.exchange.config.AuditProperties;

@Service
public class AuditEventService {

	private static final int DEFAULT_LIMIT = 100;

	private final AuditEventRepository auditEventRepository;
	private final AccountRepository accountRepository;
	private final IdGenerator idGenerator;
	private final AuditEventMasker auditEventMasker;
	private final AuditProperties auditProperties;

	public AuditEventService(
			AuditEventRepository auditEventRepository,
			AccountRepository accountRepository,
			IdGenerator idGenerator,
			AuditEventMasker auditEventMasker,
			AuditProperties auditProperties) {
		this.auditEventRepository = auditEventRepository;
		this.accountRepository = accountRepository;
		this.idGenerator = idGenerator;
		this.auditEventMasker = auditEventMasker;
		this.auditProperties = auditProperties;
	}

	public void record(
			String accountId,
			String userId,
			AuditEventType eventType,
			String subjectType,
			String subjectId,
			String summary,
			Instant occurredAt) {
		auditEventRepository.save(new AuditEvent(
				idGenerator.newAuditEventId(),
				accountId,
				userId,
				eventType,
				subjectType,
				auditEventMasker.mask(subjectId),
				auditEventMasker.mask(summary),
				occurredAt));
	}

	public AuditEventListResponse getEvents(String accountId) {
		MockUsdAccount account = accountRepository.findAccount(accountId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MOCK_ACCOUNT_NOT_FOUND));
		List<AuditEventResponse> events = auditEventRepository.findByAccountId(account.accountId(), DEFAULT_LIMIT)
				.stream()
				.map(this::toResponse)
				.toList();
		return new AuditEventListResponse(account.accountId(), events.size(), events, Instant.now());
	}

	public int purgeExpiredEvents() {
		Instant cutoff = Instant.now().minus(java.time.Duration.ofDays(auditProperties.retentionDays()));
		return auditEventRepository.deleteOccurredBefore(cutoff);
	}

	private AuditEventResponse toResponse(AuditEvent event) {
		return new AuditEventResponse(
				event.auditEventId(),
				event.eventType(),
				event.subjectType(),
				event.subjectId(),
				event.summary(),
				event.occurredAt());
	}
}
