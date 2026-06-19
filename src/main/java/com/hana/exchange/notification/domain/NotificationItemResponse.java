package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

import com.hana.exchange.alert.domain.AlertGlossaryTerm;

public record NotificationItemResponse(
		String notificationId,
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
		List<AlertGlossaryTerm> glossaryTerms,
		List<String> translationQualityFlags,
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
