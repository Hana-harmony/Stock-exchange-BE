package com.hana.exchange.notification.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.notification.domain.NotificationItem;

public interface NotificationRepository {

	boolean existsForEventAndAccount(String eventId, String accountId);

	boolean existsForSubjectAndAccount(String subjectType, String subjectId, String accountId);

	void save(NotificationItem item);

	List<NotificationItem> findRetryableForDelivery(int limit, int maxAttemptCount);

	List<NotificationItem> findByAccountId(String accountId);

	Optional<NotificationItem> findByAccountIdAndNotificationId(String accountId, String notificationId);
}
