package com.hana.exchange.account.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockCashLedgerEntry;
import com.hana.exchange.account.domain.MockUsdAccount;

@SpringBootTest
class JdbcAccountRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void savesUserAccountAndCashLedgerInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-DBTEST00001",
				"dbtrader01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-DBTEST00001",
				user.userId(),
				"USD",
				new BigDecimal("0.00"),
				now,
				now);

		accountRepository.saveNewAccount(user, account);
		MockUsdAccount updatedAccount = account.deposit(new BigDecimal("125.50"), now.plusSeconds(60));
		accountRepository.saveAccount(updatedAccount);
		accountRepository.saveLedgerEntry(new MockCashLedgerEntry(
				"LED-DBTEST00001",
				account.accountId(),
				"MOCK_USD_DEPOSIT",
				new BigDecimal("125.50"),
				new BigDecimal("125.50"),
				now.plusSeconds(60)));

		assertThat(accountRepository.existsByUsername("dbtrader01")).isTrue();
		assertThat(accountRepository.findUserById(user.userId())).contains(user);
		assertThat(accountRepository.findAccount(account.accountId()))
				.hasValueSatisfying(savedAccount -> {
					assertThat(savedAccount.accountId()).isEqualTo(account.accountId());
					assertThat(savedAccount.cashBalanceUsd()).isEqualByComparingTo("125.50");
				});
	}
}
