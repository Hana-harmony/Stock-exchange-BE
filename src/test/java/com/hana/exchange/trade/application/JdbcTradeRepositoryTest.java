package com.hana.exchange.trade.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.trade.domain.MockHolding;
import com.hana.exchange.trade.domain.MockTradeLedgerEntry;
import com.hana.exchange.trade.domain.TradeSide;

@SpringBootTest
class JdbcTradeRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TradeRepository tradeRepository;

	@Test
	void savesHoldingAndTradeLedgerInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-TRADEDB001",
				"tradedb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-TRADEDB001",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		MockHolding holding = MockHolding.empty(
						account.accountId(),
						user.userId(),
						"005930",
						"Samsung Electronics",
						now)
				.buy(2, new BigDecimal("50.00"), now.plusSeconds(10));
		MockTradeLedgerEntry trade = new MockTradeLedgerEntry(
				"TRD-TRADEDB001",
				account.accountId(),
				user.userId(),
				"005930",
				"Samsung Electronics",
				TradeSide.BUY,
				2,
				new BigDecimal("50.00"),
				new BigDecimal("100.00"),
				new BigDecimal("0.00"),
				2,
				new BigDecimal("50.00"),
				new BigDecimal("400.00"),
				now.plusSeconds(10));

		tradeRepository.saveHolding(holding);
		tradeRepository.saveTrade(trade);

		assertThat(tradeRepository.findHolding(account.accountId(), "005930"))
				.hasValueSatisfying(savedHolding -> {
					assertThat(savedHolding.quantity()).isEqualTo(2);
					assertThat(savedHolding.averagePriceUsd()).isEqualByComparingTo("50.00");
				});
		assertThat(tradeRepository.findHoldingsByStockCodes(List.of("005930")))
				.extracting(MockHolding::accountId)
				.contains(account.accountId());
		assertThat(tradeRepository.findTrades(account.accountId())).containsExactly(trade);
		assertThat(tradeRepository.findRecentTrades(account.accountId(), 1)).containsExactly(trade);
	}

	@Test
	void zeroQuantityHoldingDeletesRow() {
		Instant now = Instant.parse("2026-06-18T06:30:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-TRADEDB002",
				"tradedb02",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-TRADEDB002",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		MockHolding holding = MockHolding.empty(
						account.accountId(),
						user.userId(),
						"000660",
						"SK Hynix",
						now)
				.buy(1, new BigDecimal("80.00"), now.plusSeconds(10));

		tradeRepository.saveHolding(holding);
		tradeRepository.saveHolding(holding.sell(1, now.plusSeconds(20)));

		assertThat(tradeRepository.findHolding(account.accountId(), "000660")).isEmpty();
	}
}
