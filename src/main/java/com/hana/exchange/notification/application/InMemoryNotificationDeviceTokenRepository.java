package com.hana.exchange.notification.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.hana.exchange.notification.domain.NotificationDevicePlatform;
import com.hana.exchange.notification.domain.NotificationDeviceToken;

@Repository
@Profile("memory")
public class InMemoryNotificationDeviceTokenRepository implements NotificationDeviceTokenRepository {

	private final Map<String, NotificationDeviceToken> tokensById = new ConcurrentHashMap<>();

	@Override
	public Optional<NotificationDeviceToken> findByAccountIdAndPlatformAndTokenHash(
			String accountId,
			NotificationDevicePlatform platform,
			String tokenHash) {
		return tokensById.values()
				.stream()
				.filter(token -> token.accountId().equals(accountId))
				.filter(token -> token.platform() == platform)
				.filter(token -> token.tokenHash().equals(tokenHash))
				.findFirst();
	}

	@Override
	public Optional<NotificationDeviceToken> findByAccountIdAndDeviceTokenId(String accountId, String deviceTokenId) {
		return Optional.ofNullable(tokensById.get(deviceTokenId))
				.filter(token -> token.accountId().equals(accountId));
	}

	@Override
	public List<NotificationDeviceToken> findByAccountId(String accountId) {
		return tokensById.values()
				.stream()
				.filter(token -> token.accountId().equals(accountId))
				.sorted(Comparator.comparing(NotificationDeviceToken::lastSeenAt).reversed())
				.toList();
	}

	@Override
	public void save(NotificationDeviceToken deviceToken) {
		tokensById.put(deviceToken.deviceTokenId(), deviceToken);
	}
}
