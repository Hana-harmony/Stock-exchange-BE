package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

public record NotificationItem(
		String notificationId,
		String accountId,
		String userId,
		String eventId,
		String sourceType,
		String title,
		String summary,
		String originalUrl,
		String primaryStockCode,
		List<String> matchedStockCodes,
		List<String> matchReasons,
		boolean read,
		Instant createdAt,
		Instant readAt
) {
	public NotificationItem markRead(Instant readAt) {
		return new NotificationItem(
				notificationId,
				accountId,
				userId,
				eventId,
				sourceType,
				title,
				summary,
				originalUrl,
				primaryStockCode,
				matchedStockCodes,
				matchReasons,
				true,
				createdAt,
				readAt);
	}
}
