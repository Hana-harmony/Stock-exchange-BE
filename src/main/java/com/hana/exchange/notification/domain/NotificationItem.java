package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

public record NotificationItem(
		String notificationId,
		String accountId,
		String userId,
		String eventId,
		String subjectType,
		String subjectId,
		String sourceType,
		String title,
		String summary,
		String originalUrl,
		String primaryStockCode,
		List<String> matchedStockCodes,
		List<String> matchReasons,
		NotificationDeliveryStatus deliveryStatus,
		String deliveryProvider,
		int deliveryAttemptCount,
		Instant deliveredAt,
		String lastDeliveryError,
		boolean read,
		Instant createdAt,
		Instant readAt
) {
	public NotificationItem markDelivery(NotificationDeliveryResult deliveryResult) {
		return new NotificationItem(
				notificationId,
				accountId,
				userId,
				eventId,
				subjectType,
				subjectId,
				sourceType,
				title,
				summary,
				originalUrl,
				primaryStockCode,
				matchedStockCodes,
				matchReasons,
				deliveryResult.status(),
				deliveryResult.provider(),
				deliveryAttemptCount + 1,
				deliveryResult.deliveredAt(),
				deliveryResult.errorMessage(),
				read,
				createdAt,
				readAt);
	}

	public NotificationItem markRead(Instant readAt) {
		return new NotificationItem(
				notificationId,
				accountId,
				userId,
				eventId,
				subjectType,
				subjectId,
				sourceType,
				title,
				summary,
				originalUrl,
				primaryStockCode,
				matchedStockCodes,
				matchReasons,
				deliveryStatus,
				deliveryProvider,
				deliveryAttemptCount,
				deliveredAt,
				lastDeliveryError,
				true,
				createdAt,
				readAt);
	}
}
