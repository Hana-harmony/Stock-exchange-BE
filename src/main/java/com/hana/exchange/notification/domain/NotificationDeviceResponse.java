package com.hana.exchange.notification.domain;

import java.time.Instant;

public record NotificationDeviceResponse(
		String deviceTokenId,
		NotificationDevicePlatform platform,
		String provider,
		String tokenHash,
		String maskedToken,
		String appVersion,
		String locale,
		boolean active,
		Instant registeredAt,
		Instant lastSeenAt,
		Instant disabledAt
) {
}
