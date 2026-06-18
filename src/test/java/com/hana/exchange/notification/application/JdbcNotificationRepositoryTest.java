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
				"ALERT_EVENT",
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
		assertThat(notificationRepository.existsForSubjectAndAccount("ALERT_EVENT", event.eventId(), account.accountId())).isTrue();
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

	@Test
	void findsRetryableNotificationsByCreatedOrderUnderMaxAttemptCount() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser(
				"USR-NOTIFYDB02",
				"notifydb02",
				"salt",
				"hash",
				now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-NOTIFYDB02",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		AlertEvent firstEvent = event("NOTIFY-DB-EVENT-02", "notify-db-key-02", now);
		AlertEvent secondEvent = event("NOTIFY-DB-EVENT-03", "notify-db-key-03", now.plusSeconds(1));
		AlertEvent exhaustedEvent = event("NOTIFY-DB-EVENT-04", "notify-db-key-04", now.plusSeconds(2));
		AlertEvent deliveredEvent = event("NOTIFY-DB-EVENT-05", "notify-db-key-05", now.plusSeconds(3));
		alertEventRepository.save(firstEvent, new AlertEventMatchResult(firstEvent, List.of(), now.plusSeconds(10)));
		alertEventRepository.save(secondEvent, new AlertEventMatchResult(secondEvent, List.of(), now.plusSeconds(11)));
		alertEventRepository.save(exhaustedEvent, new AlertEventMatchResult(exhaustedEvent, List.of(), now.plusSeconds(12)));
		alertEventRepository.save(deliveredEvent, new AlertEventMatchResult(deliveredEvent, List.of(), now.plusSeconds(13)));
		notificationRepository.save(notification("NTF-NOTIFYDB02", account, firstEvent, NotificationDeliveryStatus.FAILED, 1, now.plusSeconds(20)));
		notificationRepository.save(notification("NTF-NOTIFYDB03", account, secondEvent, NotificationDeliveryStatus.PENDING, 0, now.plusSeconds(21)));
		notificationRepository.save(notification("NTF-NOTIFYDB04", account, exhaustedEvent, NotificationDeliveryStatus.FAILED, 3, now.plusSeconds(22)));
		notificationRepository.save(notification("NTF-NOTIFYDB05", account, deliveredEvent, NotificationDeliveryStatus.DELIVERED, 1, now.plusSeconds(23)));

		assertThat(notificationRepository.findRetryableForDelivery(10, 3))
				.extracting(NotificationItem::notificationId)
				.contains("NTF-NOTIFYDB02", "NTF-NOTIFYDB03")
				.doesNotContain("NTF-NOTIFYDB04", "NTF-NOTIFYDB05");
		assertThat(notificationRepository.findRetryableForDelivery(1, 3))
				.extracting(NotificationItem::notificationId)
				.containsExactly("NTF-NOTIFYDB02");
	}

	private AlertEvent event(String eventId, Instant now) {
		return event(eventId, "notify-db-key-01", now);
	}

	private AlertEvent event(String eventId, String idempotencyKey, Instant now) {
		return new AlertEvent(
				eventId,
				idempotencyKey,
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

	private NotificationItem notification(
			String notificationId,
			MockUsdAccount account,
			AlertEvent event,
			NotificationDeliveryStatus status,
			int attemptCount,
			Instant createdAt) {
		return new NotificationItem(
				notificationId,
				account.accountId(),
				account.userId(),
				event.eventId(),
				"ALERT_EVENT",
				event.eventId(),
				event.sourceType(),
				event.title(),
				event.summary(),
				event.originalUrl(),
				event.stockCode(),
				List.of("005930"),
				List.of("WATCHLIST"),
				status,
				null,
				attemptCount,
				null,
				null,
				false,
				createdAt,
				null);
	}
}
