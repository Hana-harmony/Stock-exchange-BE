package com.hana.exchange.notification.application;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.hana.exchange.notification.domain.NotificationDevicePlatform;
import com.hana.exchange.notification.domain.NotificationDeviceToken;

@Repository
@Profile("!memory")
public class JdbcNotificationDeviceTokenRepository implements NotificationDeviceTokenRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcNotificationDeviceTokenRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<NotificationDeviceToken> findByAccountIdAndPlatformAndTokenHash(
			String accountId,
			NotificationDevicePlatform platform,
			String tokenHash) {
		return jdbcTemplate.query(
				select() + " WHERE account_id = ? AND platform = ? AND token_hash = ?",
				(resultSet, rowNumber) -> token(resultSet),
				accountId,
				platform.name(),
				tokenHash)
				.stream()
				.findFirst();
	}

	@Override
	public Optional<NotificationDeviceToken> findByAccountIdAndDeviceTokenId(String accountId, String deviceTokenId) {
		return jdbcTemplate.query(
				select() + " WHERE account_id = ? AND device_token_id = ?",
				(resultSet, rowNumber) -> token(resultSet),
				accountId,
				deviceTokenId)
				.stream()
				.findFirst();
	}

	@Override
	public List<NotificationDeviceToken> findByAccountId(String accountId) {
		return jdbcTemplate.query(
				select() + " WHERE account_id = ? ORDER BY last_seen_at DESC",
				(resultSet, rowNumber) -> token(resultSet),
				accountId);
	}

	@Override
	public void save(NotificationDeviceToken deviceToken) {
		int updated = jdbcTemplate.update(
				"UPDATE notification_device_tokens "
						+ "SET provider = ?, masked_token = ?, app_version = ?, locale = ?, active = ?, "
						+ "last_seen_at = ?, disabled_at = ? "
						+ "WHERE device_token_id = ?",
				deviceToken.provider(),
				deviceToken.maskedToken(),
				deviceToken.appVersion(),
				deviceToken.locale(),
				deviceToken.active(),
				timestamp(deviceToken.lastSeenAt()),
				timestampOrNull(deviceToken.disabledAt()),
				deviceToken.deviceTokenId());
		if (updated == 0) {
			insert(deviceToken);
		}
	}

	private void insert(NotificationDeviceToken deviceToken) {
		jdbcTemplate.update(
				"INSERT INTO notification_device_tokens "
						+ "(device_token_id, account_id, user_id, platform, provider, token_hash, masked_token, "
						+ "app_version, locale, active, registered_at, last_seen_at, disabled_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				deviceToken.deviceTokenId(),
				deviceToken.accountId(),
				deviceToken.userId(),
				deviceToken.platform().name(),
				deviceToken.provider(),
				deviceToken.tokenHash(),
				deviceToken.maskedToken(),
				deviceToken.appVersion(),
				deviceToken.locale(),
				deviceToken.active(),
				timestamp(deviceToken.registeredAt()),
				timestamp(deviceToken.lastSeenAt()),
				timestampOrNull(deviceToken.disabledAt()));
	}

	private NotificationDeviceToken token(ResultSet resultSet) throws SQLException {
		return new NotificationDeviceToken(
				resultSet.getString("device_token_id"),
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				NotificationDevicePlatform.valueOf(resultSet.getString("platform")),
				resultSet.getString("provider"),
				resultSet.getString("token_hash"),
				resultSet.getString("masked_token"),
				resultSet.getString("app_version"),
				resultSet.getString("locale"),
				resultSet.getBoolean("active"),
				instant(resultSet, "registered_at"),
				instant(resultSet, "last_seen_at"),
				instantOrNull(resultSet, "disabled_at"));
	}

	private String select() {
		return "SELECT device_token_id, account_id, user_id, platform, provider, token_hash, masked_token, "
				+ "app_version, locale, active, registered_at, last_seen_at, disabled_at "
				+ "FROM notification_device_tokens";
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private Timestamp timestampOrNull(Instant instant) {
		if (instant == null) {
			return null;
		}
		return timestamp(instant);
	}

	private Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}

	private Instant instantOrNull(ResultSet resultSet, String column) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(column);
		if (timestamp == null) {
			return null;
		}
		return timestamp.toInstant();
	}
}
