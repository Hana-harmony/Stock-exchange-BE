package com.hana.exchange.alert.application;

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
import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;
import com.hana.exchange.alert.domain.AlertTargetResponse;

@SpringBootTest
class JdbcAlertEventRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AlertEventRepository alertEventRepository;

	@Test
	void savesFindsAndIndexesAlertEventMatchResultInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-ALERTDB001",
				"alertdb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-ALERTDB001",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		AlertEvent event = event("ALERT-DB-01", "alert-db-key-01", "005930", List.of("000660"), now);
		AlertTargetResponse target = new AlertTargetResponse(
				account.accountId(),
				user.userId(),
				List.of("WATCHLIST", "HOLDER"),
				List.of("005930", "000660"));
		AlertEventMatchResult result = new AlertEventMatchResult(event, List.of(target), now.plusSeconds(2));

		alertEventRepository.save(event, result);

		assertThat(alertEventRepository.findByIdempotencyKey("alert-db-key-01"))
				.hasValueSatisfying(saved -> assertResult(saved, event, target));
		assertThat(alertEventRepository.findByEventId("ALERT-DB-01"))
				.hasValueSatisfying(saved -> assertResult(saved, event, target));
		assertThat(alertEventRepository.findByStockCode("005930"))
				.extracting(saved -> saved.event().eventId())
				.contains("ALERT-DB-01");
		assertThat(alertEventRepository.findByStockCode("000660"))
				.extracting(saved -> saved.event().eventId())
				.contains("ALERT-DB-01");
	}

	@Test
	void stockFeedResultsAreSortedByPublishedAtDescending() {
		Instant older = Instant.parse("2026-06-18T05:00:00Z");
		Instant newer = Instant.parse("2026-06-18T06:00:00Z");
		AlertEvent olderEvent = event("ALERT-DB-OLD", "alert-db-key-old", "035420", List.of(), older);
		AlertEvent newerEvent = event("ALERT-DB-NEW", "alert-db-key-new", "035420", List.of(), newer);

		alertEventRepository.save(olderEvent, new AlertEventMatchResult(olderEvent, List.of(), older.plusSeconds(1)));
		alertEventRepository.save(newerEvent, new AlertEventMatchResult(newerEvent, List.of(), newer.plusSeconds(1)));

		assertThat(alertEventRepository.findByStockCode("035420"))
				.extracting(result -> result.event().eventId())
				.containsSubsequence("ALERT-DB-NEW", "ALERT-DB-OLD");
	}

	private void assertResult(AlertEventMatchResult saved, AlertEvent event, AlertTargetResponse target) {
		assertThat(saved.event()).isEqualTo(event);
		assertThat(saved.targets()).containsExactly(target);
		assertThat(saved.targetCount()).isEqualTo(1);
	}

	private AlertEvent event(String eventId, String idempotencyKey, String stockCode, List<String> relatedStocks, Instant publishedAt) {
		return new AlertEvent(
				eventId,
				idempotencyKey,
				"NEWS",
				"Samsung supply chain update",
				"Translated AI summary for local investors",
				"https://news.example.com/original",
				stockCode,
				relatedStocks,
				"POSITIVE",
				"HIGH",
				"MEDIUM",
				true,
				true,
				publishedAt,
				publishedAt.plusSeconds(1));
	}
}
