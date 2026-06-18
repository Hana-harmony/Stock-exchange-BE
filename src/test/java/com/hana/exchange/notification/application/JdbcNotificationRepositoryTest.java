package com.hana.exchange.notification.application;

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
import com.hana.exchange.alert.application.AlertEventRepository;
import com.hana.exchange.alert.domain.AlertEvent;
import com.hana.exchange.alert.domain.AlertEventMatchResult;
import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

@SpringBootTest
class JdbcNotificationRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private AlertEventRepository alertEventRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Test
	void savesFindsAndMarksNotificationReadInDatabase() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-NOTIFYDB01",
				"notifydb01",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-NOTIFYDB01",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		AlertEvent event = event("NOTIFY-DB-EVENT-01", now);
		alertEventRepository.save(event, new AlertEventMatchResult(event, List.of(), now.plusSeconds(1)));
		NotificationItem item = new NotificationItem(
				"NTF-NOTIFYDB01",
				account.accountId(),
				user.userId(),
				event.eventId(),
				event.sourceType(),
				event.title(),
				event.summary(),
				event.originalUrl(),
				event.stockCode(),
				List.of("005930", "000660"),
				List.of("WATCHLIST", "HOLDER"),
				NotificationDeliveryStatus.PENDING,
				null,
				0,
				null,
				null,
				false,
				now.plusSeconds(2),
				null).markDelivery(NotificationDeliveryResult.delivered("LOCAL_NOOP_PUSH", now.plusSeconds(3)));

		notificationRepository.save(item);

		assertThat(notificationRepository.existsForEventAndAccount(event.eventId(), account.accountId())).isTrue();
		assertThat(notificationRepository.findByAccountId(account.accountId())).containsExactly(item);
		assertThat(notificationRepository.findByAccountIdAndNotificationId(account.accountId(), item.notificationId()))
				.contains(item);

		NotificationItem readItem = item.markRead(now.plusSeconds(4));
		notificationRepository.save(readItem);

		assertThat(notificationRepository.findByAccountIdAndNotificationId(account.accountId(), item.notificationId()))
				.hasValueSatisfying(saved -> {
					assertThat(saved.read()).isTrue();
					assertThat(saved.readAt()).isEqualTo(now.plusSeconds(4));
					assertThat(saved.matchedStockCodes()).containsExactly("005930", "000660");
					assertThat(saved.matchReasons()).containsExactly("WATCHLIST", "HOLDER");
				});
	}

	private AlertEvent event(String eventId, Instant now) {
		return new AlertEvent(
				eventId,
				"notify-db-key-01",
				"DISCLOSURE",
				"Samsung disclosure update",
				"Translated AI summary for local investors",
				"https://news.example.com/original",
				"005930",
				List.of(),
				"NEUTRAL",
				"HIGH",
				"LOW",
				true,
				false,
				now,
				now.plusSeconds(1));
	}
}
