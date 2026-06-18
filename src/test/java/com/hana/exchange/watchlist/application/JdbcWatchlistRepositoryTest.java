package com.hana.exchange.watchlist.application;

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
import com.hana.exchange.watchlist.domain.WatchlistItem;

@SpringBootTest
class JdbcWatchlistRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private WatchlistRepository watchlistRepository;

	@Test
	void savesFindsAndDeletesWatchlistItemInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-WATCHDB001",
				"watchdb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-WATCHDB001",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		WatchlistItem item = new WatchlistItem(
				account.accountId(),
				user.userId(),
				"005930",
				"Samsung Electronics",
				"KOSPI",
				"WATCHLIST_ALERT_TARGET",
				now.plusSeconds(10));

		watchlistRepository.saveItem(item);

		assertThat(watchlistRepository.findItem(account.accountId(), "005930")).contains(item);
		assertThat(watchlistRepository.findItems(account.accountId())).containsExactly(item);
		assertThat(watchlistRepository.findItemsByStockCodes(List.of("005930")))
				.extracting(WatchlistItem::accountId)
				.contains(account.accountId());

		assertThat(watchlistRepository.deleteItem(account.accountId(), "005930")).isTrue();
		assertThat(watchlistRepository.findItem(account.accountId(), "005930")).isEmpty();
		assertThat(watchlistRepository.deleteItem(account.accountId(), "005930")).isFalse();
	}

	@Test
	void saveItemUpdatesMetadataWithoutChangingAddedAt() {
		Instant now = Instant.parse("2026-06-18T07:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-WATCHDB002",
				"watchdb02",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-WATCHDB002",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		WatchlistItem item = new WatchlistItem(
				account.accountId(),
				user.userId(),
				"000660",
				"SK Hynix",
				"KOSPI",
				"WATCHLIST_ALERT_TARGET",
				now.plusSeconds(10));
		WatchlistItem updated = new WatchlistItem(
				account.accountId(),
				user.userId(),
				"000660",
				"SK hynix Inc.",
				"KOSPI",
				"WATCHLIST_ALERT_TARGET",
				now.plusSeconds(20));

		watchlistRepository.saveItem(item);
		watchlistRepository.saveItem(updated);

		assertThat(watchlistRepository.findItem(account.accountId(), "000660"))
				.hasValueSatisfying(saved -> {
					assertThat(saved.stockName()).isEqualTo("SK hynix Inc.");
					assertThat(saved.addedAt()).isEqualTo(item.addedAt());
				});
	}
}
