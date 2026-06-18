package com.hana.exchange.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hana.exchange.notification.domain.NotificationDeliveryResult;
import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

class NotificationServiceTest {

	private final InMemoryNotificationRepository notificationRepository = new InMemoryNotificationRepository();

	@Test
	void dispatchRetryablePushNotificationsRetriesPendingAndFailedItemsUnderMaxAttemptCount() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		NotificationItem pending = notification("NTF-RETRY-001", "EVT-RETRY-001", NotificationDeliveryStatus.PENDING, 0, now);
		NotificationItem failed = notification("NTF-RETRY-002", "EVT-RETRY-002", NotificationDeliveryStatus.FAILED, 1, now.plusSeconds(1));
		NotificationItem exhausted = notification("NTF-RETRY-003", "EVT-RETRY-003", NotificationDeliveryStatus.FAILED, 3, now.plusSeconds(2));
		NotificationItem delivered = notification("NTF-RETRY-004", "EVT-RETRY-004", NotificationDeliveryStatus.DELIVERED, 1, now.plusSeconds(3));
		notificationRepository.save(pending);
		notificationRepository.save(failed);
		notificationRepository.save(exhausted);
		notificationRepository.save(delivered);
		NotificationService service = service(notification ->
				NotificationDeliveryResult.delivered("LOCAL_NOOP_PUSH", now.plusSeconds(10)));

		int dispatchedCount = service.dispatchRetryablePushNotifications(10, 3);

		assertThat(dispatchedCount).isEqualTo(2);
		assertThat(saved("NTF-RETRY-001").deliveryStatus()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(saved("NTF-RETRY-001").deliveryAttemptCount()).isEqualTo(1);
		assertThat(saved("NTF-RETRY-002").deliveryStatus()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(saved("NTF-RETRY-002").deliveryAttemptCount()).isEqualTo(2);
		assertThat(saved("NTF-RETRY-003").deliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
		assertThat(saved("NTF-RETRY-003").deliveryAttemptCount()).isEqualTo(3);
		assertThat(saved("NTF-RETRY-004").deliveryStatus()).isEqualTo(NotificationDeliveryStatus.DELIVERED);
		assertThat(saved("NTF-RETRY-004").deliveryAttemptCount()).isEqualTo(1);
	}

	@Test
	void dispatchRetryablePushNotificationsRecordsProviderExceptionAsFailedAttempt() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		NotificationItem pending = notification("NTF-RETRY-FAIL", "EVT-RETRY-FAIL", NotificationDeliveryStatus.PENDING, 0, now);
		notificationRepository.save(pending);
		NotificationService service = service(notification -> {
			throw new IllegalStateException("provider unavailable");
		});

		int dispatchedCount = service.dispatchRetryablePushNotifications(10, 3);

		NotificationItem savedItem = saved("NTF-RETRY-FAIL");
		assertThat(dispatchedCount).isEqualTo(1);
		assertThat(savedItem.deliveryStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
		assertThat(savedItem.deliveryProvider()).isEqualTo("PUSH_PROVIDER");
		assertThat(savedItem.deliveryAttemptCount()).isEqualTo(1);
		assertThat(savedItem.lastDeliveryError()).isEqualTo("provider unavailable");
	}

	private NotificationService service(PushNotificationSender pushNotificationSender) {
		return new NotificationService(
				null,
				notificationRepository,
				new InMemoryNotificationDeviceTokenRepository(),
				null,
				pushNotificationSender,
				null);
	}

	private NotificationItem saved(String notificationId) {
		return notificationRepository.findByAccountIdAndNotificationId("ACC-RETRY-001", notificationId).orElseThrow();
	}

	private NotificationItem notification(
			String notificationId,
			String eventId,
			NotificationDeliveryStatus status,
			int attemptCount,
			Instant createdAt) {
		return new NotificationItem(
				notificationId,
				"ACC-RETRY-001",
				"USR-RETRY-001",
				eventId,
				"ALERT_EVENT",
				eventId,
				"DISCLOSURE",
				"Samsung disclosure update",
				"Translated AI summary",
				"https://news.example.com/original",
				"005930",
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
