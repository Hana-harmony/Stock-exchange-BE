package com.hana.exchange.account.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.account.domain.RefreshSession;

@SpringBootTest
class JdbcRefreshSessionRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private RefreshSessionRepository refreshSessionRepository;

	@Test
	void savesAndRevokesRefreshSessionInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-DBTEST00002",
				"dbtrader02",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-DBTEST00002",
				user.userId(),
				"USD",
				new BigDecimal("0.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		RefreshSession session = new RefreshSession(
				"SES-DBTEST00002",
				user.userId(),
				account.accountId(),
				"refresh-token-hash-02",
				now,
				now.plusSeconds(3600),
				null,
				null);

		refreshSessionRepository.save(session);
		assertThat(refreshSessionRepository.findByRefreshTokenHash(session.refreshTokenHash()))
				.hasValueSatisfying(savedSession -> assertThat(savedSession.activeAt(now.plusSeconds(30))).isTrue());

		RefreshSession revokedSession = session.revoke(now.plusSeconds(120), "SES-DBTEST00003");
		refreshSessionRepository.save(revokedSession);
		assertThat(refreshSessionRepository.findByRefreshTokenHash(session.refreshTokenHash()))
				.hasValueSatisfying(savedSession -> {
					assertThat(savedSession.activeAt(now.plusSeconds(130))).isFalse();
					assertThat(savedSession.replacedBySessionId()).isEqualTo("SES-DBTEST00003");
				});
	}
}
