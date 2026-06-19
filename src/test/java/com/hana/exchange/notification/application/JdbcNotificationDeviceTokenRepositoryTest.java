package com.hana.exchange.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.hana.exchange.account.application.AccountRepository;
import com.hana.exchange.account.domain.ExchangeUser;
import com.hana.exchange.account.domain.MockUsdAccount;
import com.hana.exchange.notification.domain.NotificationDevicePlatform;
import com.hana.exchange.notification.domain.NotificationDeviceToken;

@SpringBootTest
class JdbcNotificationDeviceTokenRepositoryTest {

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private NotificationDeviceTokenRepository repository;

	@Test
	void savesRefreshesAndDisablesDeviceToken() {
		Instant now = Instant.parse("2026-06-18T06:00:00Z");
		ExchangeUser user = new ExchangeUser("USR-NOTIFYDV01", "notifydv01", "salt", "hash", now);
		MockUsdAccount account = new MockUsdAccount(
				"ACC-NOTIFYDV01",
				user.userId(),
				"USD",
				new BigDecimal("500.00"),
				now,
				now);
		accountRepository.saveNewAccount(user, account);
		NotificationDeviceToken token = new NotificationDeviceToken(
				"NTD-NOTIFYDV01",
				account.accountId(),
				user.userId(),
				NotificationDevicePlatform.ANDROID,
				"FCM_PUSH",
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				"android...123456",
				"1.0.0",
				"en_US",
				true,
				now.plusSeconds(1),
				now.plusSeconds(1),
				null);

		repository.save(token);

		assertThat(repository.findByAccountId(account.accountId())).containsExactly(token);
		assertThat(repository.findByAccountIdAndPlatformAndTokenHash(
				account.accountId(),
				NotificationDevicePlatform.ANDROID,
				token.tokenHash())).contains(token);

		NotificationDeviceToken refreshed = token.seen(
				"FCM_PUSH",
				"android...123456",
				null,
				"1.0.1",
				"en_US",
				now.plusSeconds(10));
		repository.save(refreshed);

		assertThat(repository.findByAccountId(account.accountId()))
				.hasSize(1)
				.first()
				.satisfies(saved -> {
					assertThat(saved.appVersion()).isEqualTo("1.0.1");
					assertThat(saved.lastSeenAt()).isEqualTo(now.plusSeconds(10));
					assertThat(saved.active()).isTrue();
				});

		NotificationDeviceToken disabled = refreshed.disabled(now.plusSeconds(20));
		repository.save(disabled);

		assertThat(repository.findByAccountIdAndDeviceTokenId(account.accountId(), token.deviceTokenId()))
				.hasValueSatisfying(saved -> {
					assertThat(saved.active()).isFalse();
					assertThat(saved.disabledAt()).isEqualTo(now.plusSeconds(20));
				});
	}
}
