package com.hana.exchange.notification.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.notification.domain.NotificationItem;

@Repository
@Profile("memory")
public class InMemoryNotificationRepository implements NotificationRepository {

	private final Map<String, NotificationItem> itemsById = new ConcurrentHashMap<>();
	private final Map<String, String> itemIdByEventAndAccount = new ConcurrentHashMap<>();

	@Override
	public boolean existsForEventAndAccount(String eventId, String accountId) {
		return itemIdByEventAndAccount.containsKey(key(eventId, accountId));
	}

	@Override
	public void save(NotificationItem item) {
		itemsById.put(item.notificationId(), item);
		itemIdByEventAndAccount.put(key(item.eventId(), item.accountId()), item.notificationId());
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
}
