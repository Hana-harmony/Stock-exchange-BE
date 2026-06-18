package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

public record NotificationItemResponse(
		String notificationId,
		String eventId,
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
}
