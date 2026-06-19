package com.hana.exchange.notification.domain;

import java.time.Instant;
import java.util.List;

import com.hana.exchange.alert.domain.AlertGlossaryTerm;

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
	public NotificationItem {
		matchedStockCodes = matchedStockCodes == null ? List.of() : List.copyOf(matchedStockCodes);
		matchReasons = matchReasons == null ? List.of() : List.copyOf(matchReasons);
		glossaryTerms = glossaryTerms == null ? List.of() : List.copyOf(glossaryTerms);
		translationQualityFlags = translationQualityFlags == null
				? List.of()
				: List.copyOf(translationQualityFlags);
	}

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
				glossaryTerms,
				translationQualityFlags,
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
				glossaryTerms,
				translationQualityFlags,
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
