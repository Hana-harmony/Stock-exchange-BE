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
import org.springframework.transaction.annotation.Transactional;

import com.hana.exchange.notification.domain.NotificationDeliveryStatus;
import com.hana.exchange.notification.domain.NotificationItem;

@Repository
@Profile("!memory")
public class JdbcNotificationRepository implements NotificationRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcNotificationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean existsForEventAndAccount(String eventId, String accountId) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notification_items WHERE event_id = ? AND account_id = ?",
				Integer.class,
				eventId,
				accountId);
		return count != null && count > 0;
	}

	@Override
	@Transactional
	public void save(NotificationItem item) {
		int updated = jdbcTemplate.update(
				"UPDATE notification_items "
						+ "SET delivery_status = ?, delivery_provider = ?, delivery_attempt_count = ?, "
						+ "delivered_at = ?, last_delivery_error = ?, read = ?, read_at = ? "
						+ "WHERE notification_id = ?",
				item.deliveryStatus().name(),
				item.deliveryProvider(),
				item.deliveryAttemptCount(),
				timestampOrNull(item.deliveredAt()),
				item.lastDeliveryError(),
				item.read(),
				timestampOrNull(item.readAt()),
				item.notificationId());
		if (updated == 0) {
			insert(item);
			return;
		}
		replaceChildren(item);
	}

	@Override
	public List<NotificationItem> findByAccountId(String accountId) {
		return jdbcTemplate.query(
				itemSelect() + " WHERE account_id = ? ORDER BY created_at DESC",
				(resultSet, rowNumber) -> item(resultSet),
				accountId);
	}

	@Override
	public Optional<NotificationItem> findByAccountIdAndNotificationId(String accountId, String notificationId) {
		return jdbcTemplate.query(
				itemSelect() + " WHERE account_id = ? AND notification_id = ?",
				(resultSet, rowNumber) -> item(resultSet),
				accountId,
				notificationId)
				.stream()
				.findFirst();
	}

	private void insert(NotificationItem item) {
		jdbcTemplate.update(
				"INSERT INTO notification_items "
						+ "(notification_id, account_id, user_id, event_id, source_type, title, summary, original_url, "
						+ "primary_stock_code, delivery_status, delivery_provider, delivery_attempt_count, "
						+ "delivered_at, last_delivery_error, read, created_at, read_at) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				item.notificationId(),
				item.accountId(),
				item.userId(),
				item.eventId(),
				item.sourceType(),
				item.title(),
				item.summary(),
				item.originalUrl(),
				item.primaryStockCode(),
				item.deliveryStatus().name(),
				item.deliveryProvider(),
				item.deliveryAttemptCount(),
				timestampOrNull(item.deliveredAt()),
				item.lastDeliveryError(),
				item.read(),
				timestamp(item.createdAt()),
				timestampOrNull(item.readAt()));
		replaceChildren(item);
	}

	private void replaceChildren(NotificationItem item) {
		jdbcTemplate.update("DELETE FROM notification_matched_stocks WHERE notification_id = ?", item.notificationId());
		jdbcTemplate.update("DELETE FROM notification_match_reasons WHERE notification_id = ?", item.notificationId());
		for (int index = 0; index < item.matchedStockCodes().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO notification_matched_stocks (notification_id, stock_code, sort_order) "
							+ "VALUES (?, ?, ?)",
					item.notificationId(),
					item.matchedStockCodes().get(index),
					index);
		}
		for (int index = 0; index < item.matchReasons().size(); index++) {
			jdbcTemplate.update(
					"INSERT INTO notification_match_reasons (notification_id, match_reason, sort_order) "
							+ "VALUES (?, ?, ?)",
					item.notificationId(),
					item.matchReasons().get(index),
					index);
		}
	}

	private NotificationItem item(ResultSet resultSet) throws SQLException {
		String notificationId = resultSet.getString("notification_id");
		return new NotificationItem(
				notificationId,
				resultSet.getString("account_id"),
				resultSet.getString("user_id"),
				resultSet.getString("event_id"),
				resultSet.getString("source_type"),
				resultSet.getString("title"),
				resultSet.getString("summary"),
				resultSet.getString("original_url"),
				resultSet.getString("primary_stock_code"),
				matchedStockCodes(notificationId),
				matchReasons(notificationId),
				NotificationDeliveryStatus.valueOf(resultSet.getString("delivery_status")),
				resultSet.getString("delivery_provider"),
				resultSet.getInt("delivery_attempt_count"),
				instantOrNull(resultSet, "delivered_at"),
				resultSet.getString("last_delivery_error"),
				resultSet.getBoolean("read"),
				instant(resultSet, "created_at"),
				instantOrNull(resultSet, "read_at"));
	}

	private List<String> matchedStockCodes(String notificationId) {
		return jdbcTemplate.query(
				"SELECT stock_code FROM notification_matched_stocks "
						+ "WHERE notification_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("stock_code"),
				notificationId);
	}

	private List<String> matchReasons(String notificationId) {
		return jdbcTemplate.query(
				"SELECT match_reason FROM notification_match_reasons "
						+ "WHERE notification_id = ? ORDER BY sort_order ASC",
				(resultSet, rowNumber) -> resultSet.getString("match_reason"),
				notificationId);
	}

	private String itemSelect() {
		return "SELECT notification_id, account_id, user_id, event_id, source_type, title, summary, original_url, "
				+ "primary_stock_code, delivery_status, delivery_provider, delivery_attempt_count, "
				+ "delivered_at, last_delivery_error, read, created_at, read_at FROM notification_items";
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
