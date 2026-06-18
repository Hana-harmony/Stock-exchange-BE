package com.hana.exchange.notification.application;

import java.util.List;
import java.util.Optional;

import com.hana.exchange.notification.domain.NotificationDevicePlatform;
import com.hana.exchange.notification.domain.NotificationDeviceToken;

public interface NotificationDeviceTokenRepository {

	Optional<NotificationDeviceToken> findByAccountIdAndPlatformAndTokenHash(
			String accountId,
			NotificationDevicePlatform platform,
			String tokenHash);

	Optional<NotificationDeviceToken> findByAccountIdAndDeviceTokenId(String accountId, String deviceTokenId);

	List<NotificationDeviceToken> findByAccountId(String accountId);

	void save(NotificationDeviceToken deviceToken);
}
