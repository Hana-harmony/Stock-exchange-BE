package com.hana.exchange.notification.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

@Repository
@Profile("memory")
public class InMemoryNotificationRepository implements NotificationRepository {

	private final Map<String, NotificationItem> itemsById = new ConcurrentHashMap<>();
	private final Map<String, String> itemIdByEventAndAccount = new ConcurrentHashMap<>();
	private final Map<String, String> itemIdBySubjectAndAccount = new ConcurrentHashMap<>();

	@Override
	public boolean existsForEventAndAccount(String eventId, String accountId) {
		return itemIdByEventAndAccount.containsKey(key(eventId, accountId));
	}

	@Override
	public boolean existsForSubjectAndAccount(String subjectType, String subjectId, String accountId) {
		return itemIdBySubjectAndAccount.containsKey(subjectKey(subjectType, subjectId, accountId));
	}

	@Override
	public void save(NotificationItem item) {
		itemsById.put(item.notificationId(), item);
		if (item.eventId() != null) {
			itemIdByEventAndAccount.put(key(item.eventId(), item.accountId()), item.notificationId());
		}
		itemIdBySubjectAndAccount.put(subjectKey(item.subjectType(), item.subjectId(), item.accountId()), item.notificationId());
	}

	@Override
	public List<NotificationItem> findRetryableForDelivery(int limit, int maxAttemptCount) {
		return itemsById.values()
				.stream()
				.filter(item -> retryable(item, maxAttemptCount))
				.sorted(Comparator.comparing(NotificationItem::createdAt))
				.limit(limit)
				.toList();
	}

	@Override
	public List<NotificationItem> findByAccountId(String accountId) {
		return itemsById.values()
				.stream()
				.filter(item -> item.accountId().equals(accountId))
				.sorted(Comparator.comparing(NotificationItem::createdAt).reversed())
				.toList();
	}

	@Override
	public Optional<NotificationItem> findByAccountIdAndNotificationId(String accountId, String notificationId) {
		return Optional.ofNullable(itemsById.get(notificationId))
				.filter(item -> item.accountId().equals(accountId));
	}

	private String key(String eventId, String accountId) {
		return eventId + ":" + accountId;
	}

	private String subjectKey(String subjectType, String subjectId, String accountId) {
		return subjectType + ":" + subjectId + ":" + accountId;
	}

	private boolean retryable(NotificationItem item, int maxAttemptCount) {
		return item.deliveryAttemptCount() < maxAttemptCount
				&& (item.deliveryStatus() == NotificationDeliveryStatus.PENDING
				|| item.deliveryStatus() == NotificationDeliveryStatus.FAILED);
	}
}
