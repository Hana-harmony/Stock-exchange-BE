package com.hana.exchange.notification.domain;

import java.time.Instant;

public record NotificationDeviceToken(
		String deviceTokenId,
		String accountId,
		String userId,
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
	public NotificationDeviceToken seen(
			String provider,
			String maskedToken,
			String appVersion,
			String locale,
			Instant seenAt) {
		return new NotificationDeviceToken(
				deviceTokenId,
				accountId,
				userId,
				platform,
				provider,
				tokenHash,
				maskedToken,
				appVersion,
				locale,
				true,
				registeredAt,
				seenAt,
				null);
	}

	public NotificationDeviceToken disabled(Instant disabledAt) {
		return new NotificationDeviceToken(
				deviceTokenId,
				accountId,
				userId,
				platform,
				provider,
				tokenHash,
				maskedToken,
				appVersion,
				locale,
				false,
				registeredAt,
				lastSeenAt,
				disabledAt);
	}
}
