package com.hana.exchange.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.audit.domain.AuditEvent;
import com.hana.exchange.audit.domain.AuditEventType;

@SpringBootTest
class JdbcAuditEventRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AuditEventRepository auditEventRepository;

	@Test
	void savesAndFindsRecentAuditEventsInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-AUDITDB01",
				"auditdb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-AUDITDB01",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		AuditEvent older = event(account, user, "AUD-AUDITDB01", "TRD-OLD", now.plusSeconds(1));
		AuditEvent newer = event(account, user, "AUD-AUDITDB02", "TRD-NEW", now.plusSeconds(2));

		auditEventRepository.save(older);
		auditEventRepository.save(newer);

		assertThat(auditEventRepository.findByAccountId(account.accountId(), 1)).containsExactly(newer);
		assertThat(auditEventRepository.findByAccountId(account.accountId(), 10)).containsExactly(newer, older);
	}

	@Test
	void deletesAuditEventsOlderThanRetentionCutoff() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-AUDITDB02",
				"auditdb02",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-AUDITDB02",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		AuditEvent expired = event(account, user, "AUD-AUDITDB03", "TRD-EXPIRED", now.minusSeconds(10));
		AuditEvent retained = event(account, user, "AUD-AUDITDB04", "TRD-RETAINED", now.plusSeconds(10));
		auditEventRepository.save(expired);
		auditEventRepository.save(retained);

		int deletedCount = auditEventRepository.deleteOccurredBefore(now);

		assertThat(deletedCount).isEqualTo(1);
		assertThat(auditEventRepository.findByAccountId(account.accountId(), 10)).containsExactly(retained);
	}

	private AuditEvent event(MockUsdAccount account, ExchangeUser user, String auditEventId, String subjectId, Instant occurredAt) {
		return new AuditEvent(
				auditEventId,
				account.accountId(),
				user.userId(),
				AuditEventType.TRADE_EXECUTED,
				"TRADE",
				subjectId,
				"BUY 1 005930 grossUsd=50.00 realizedPnlUsd=0.00",
				occurredAt);
	}
}
