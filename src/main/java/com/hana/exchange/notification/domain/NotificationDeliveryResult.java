package com.hana.exchange.notification.domain;

import java.time.Instant;

public record NotificationDeliveryResult(
		NotificationDeliveryStatus status,
		String provider,
		Instant deliveredAt,
		String errorMessage
) {
	public static NotificationDeliveryResult delivered(String provider, Instant deliveredAt) {
		return new NotificationDeliveryResult(NotificationDeliveryStatus.DELIVERED, provider, deliveredAt, null);
	}

	public static NotificationDeliveryResult skipped(String provider, String reason) {
		return new NotificationDeliveryResult(NotificationDeliveryStatus.SKIPPED, provider, null, reason);
	}

	public static NotificationDeliveryResult failed(String provider, String errorMessage) {
		return new NotificationDeliveryResult(NotificationDeliveryStatus.FAILED, provider, null, errorMessage);
	}
}
